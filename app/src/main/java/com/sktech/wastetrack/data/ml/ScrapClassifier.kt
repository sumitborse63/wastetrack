package com.sktech.wastetrack.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val category: ScrapCategory,
    val subCategory: String = "",
    val confidence: Float,
    val rawLabels: List<String>
)

@Singleton
class ScrapClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 1. Dedicated Waste Classifier TFLite Model (Trained specifically on Waste/Scrap)
    private var wasteInterpreter: Interpreter? = null

    // 10 exact Waste Classes mapped to industrial ScrapCategory and subCategory
    private val wasteCategoriesMap = mapOf(
        0 to Triple(ScrapCategory.EWASTE, "Li-ion / Lead-Acid Batteries", "Battery & E-Waste"),
        1 to Triple(ScrapCategory.OTHER, "Mixed Solid Scrap", "Organic Scrap"),
        2 to Triple(ScrapCategory.GLASS, "Amber / Green Bottles", "Glass Bottles & Cullet"),
        3 to Triple(ScrapCategory.PAPER, "Corrugated Cardboard (OCC)", "Cardboard Boxes (OCC)"),
        4 to Triple(ScrapCategory.PAPER, "Office White Paper Shreds", "Paper Shreds & Sheets"),
        5 to Triple(ScrapCategory.METAL, "Heavy Melting Steel (HMS)", "Industrial Metal Scrap"),
        6 to Triple(ScrapCategory.OTHER, "Textiles & Fabric Rags", "Textile & Fabric Scrap"),
        7 to Triple(ScrapCategory.PLASTIC, "HDPE Drums & Containers", "Plastic / Polymer Scrap"),
        8 to Triple(ScrapCategory.OTHER, "Mixed Solid Scrap", "General Solid Waste"),
        9 to Triple(ScrapCategory.RUBBER, "Tire Shreds / Whole Tires", "Rubber & Tire Scrap")
    )

    private val wasteModelProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    // 2. ML Kit Object Detection (Backup Engine)
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

    // 3. Standard ML Kit Image Labeler (Backup Engine)
    private val defaultLabelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.15f)
        .build()
    private val imageLabeler = ImageLabeling.getClient(defaultLabelerOptions)

    // 4. OkHttp Client for Optional Gemini Cloud AI Upgrade
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    init {
        loadDedicatedWasteModel()
    }

    private fun loadDedicatedWasteModel() {
        try {
            val fileDescriptor = context.assets.openFd("waste_classifier.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            wasteInterpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun classifyImage(bitmap: Bitmap, rotationDegrees: Int): ClassificationResult = withContext(Dispatchers.Default) {
        // --- STEP 1: DEDICATED OFFLINE WASTE TFLITE MODEL EXECUTION ---
        val offlineResult = runOfflineWasteClassification(bitmap, rotationDegrees)

        // If offline classification accuracy/confidence is 90%+ (>= 0.90f) and specific, use offline result directly
        if (offlineResult.category != ScrapCategory.OTHER && offlineResult.confidence >= 0.90f) {
            return@withContext offlineResult
        }

        // --- STEP 2: IF ACCURACY IS < 90%, UPGRADE TO GEMINI FLASH VISION AI ---
        val geminiResult = classifyWithLatestGeminiVision(bitmap)
        if (geminiResult != null) {
            return@withContext geminiResult
        }

        // Fallback to offline result if Gemini Cloud is unavailable or API key is blank
        return@withContext offlineResult
    }

    private suspend fun runOfflineWasteClassification(bitmap: Bitmap, rotationDegrees: Int): ClassificationResult {
        // Engine A: Dedicated TFLite Waste Classifier
        try {
            wasteInterpreter?.let { interpreter ->
                var tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bitmap)
                tensorImage = wasteModelProcessor.process(tensorImage)

                val outputBuffer = Array(1) { FloatArray(10) }
                interpreter.run(tensorImage.buffer, outputBuffer)

                val scores = outputBuffer[0]
                val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1
                val maxScore = if (maxIndex != -1) scores[maxIndex] else 0f

                if (maxIndex in wasteCategoriesMap.keys && maxScore >= 0.25f) {
                    val (mappedCategory, subCategory, labelName) = wasteCategoriesMap[maxIndex]!!
                    val finalConfidence = if (maxScore > 0.40f) maxScore else 0.75f
                    return ClassificationResult(
                        category = mappedCategory,
                        subCategory = subCategory,
                        confidence = finalConfidence,
                        rawLabels = listOf("Offline Waste AI: $labelName (${(maxScore * 100).toInt()}%)")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Engine B: Fallback ML Kit Object Detector + Image Labeler
        val detectedLabels = mutableListOf<String>()
        var highestConfidence = 0.0f
        try {
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)

            val detectedObjects = objectDetector.process(image).await()
            for (obj in detectedObjects) {
                for (label in obj.labels) {
                    detectedLabels.add(label.text.lowercase())
                    if (label.confidence > highestConfidence) {
                        highestConfidence = label.confidence
                    }
                }
            }

            val imageLabels = imageLabeler.process(image).await()
            for (label in imageLabels) {
                detectedLabels.add(label.text.lowercase())
                if (label.confidence > highestConfidence) {
                    highestConfidence = label.confidence
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val category = mapLabelsToCategory(detectedLabels)
        val finalConfidence = if (highestConfidence > 0f) highestConfidence else 0.70f

        return ClassificationResult(
            category = category,
            subCategory = category.subCategories.firstOrNull() ?: "",
            confidence = finalConfidence,
            rawLabels = detectedLabels.distinct()
        )
    }

    private suspend fun classifyWithLatestGeminiVision(bitmap: Bitmap): ClassificationResult? {
        val apiKey = Constants.GEMINI_API_KEY
        if (apiKey.isBlank()) return null

        return withContext(Dispatchers.IO) {
            val geminiModels = listOf(
                "gemini-3.6-flash",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
            )

            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                val prompt = """
                    You are an expert industrial scrap waste classifier. Analyze this photo.
                    Return EXACTLY ONE of these categories: METAL, PLASTIC, WOOD, PAPER, GLASS, EWASTE, CHEMICAL, RUBBER, OTHER.
                    Identify the exact specific subCategory (e.g. "Copper Wire / Cable", "Aluminum Ingot / Extrusion", "Heavy Melting Steel (HMS)", "Stainless Steel (304/316)", "Brass Scrap", "PET Bottles & Sheets", "HDPE Drums & Containers", "Corrugated Cardboard (OCC)", "Printed Circuit Boards (PCB)", "Tire Shreds / Whole Tires").
                    Return a JSON object: {"category": "METAL", "subCategory": "Copper Wire / Cable", "confidence": 0.98, "details": "High grade copper wire scrap"}
                """.trimIndent()

                val jsonRequestBody = """
                    {
                      "contents": [{
                        "parts": [
                          {"text": ${gson.toJson(prompt)}},
                          {"inline_data": {"mime_type": "image/jpeg", "data": "$base64Image"}}
                        ]
                      }],
                      "generationConfig": {"response_mime_type": "application/json"}
                    }
                """.trimIndent()

                for (modelName in geminiModels) {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonRequestBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    try {
                        val response = okHttpClient.newCall(request).execute()
                        response.use { resp ->
                            if (resp.isSuccessful) {
                                val bodyString = resp.body?.string() ?: return@use
                                val jsonResponse = gson.fromJson(bodyString, JsonObject::class.java)
                                var textContent = jsonResponse
                                    .getAsJsonArray("candidates")
                                    ?.get(0)?.asJsonObject
                                    ?.getAsJsonObject("content")
                                    ?.getAsJsonArray("parts")
                                    ?.get(0)?.asJsonObject
                                    ?.get("text")?.asString ?: return@use

                                textContent = textContent.trim()
                                if (textContent.startsWith("```json")) {
                                    textContent = textContent.removePrefix("```json").removeSuffix("```").trim()
                                } else if (textContent.startsWith("```")) {
                                    textContent = textContent.removePrefix("```").removeSuffix("```").trim()
                                }

                                val parsed = gson.fromJson(textContent, JsonObject::class.java)
                                val catString = parsed.get("category")?.asString?.uppercase() ?: "OTHER"
                                val subCatString = parsed.get("subCategory")?.asString ?: ""
                                val conf = parsed.get("confidence")?.asFloat ?: 0.98f
                                val detail = parsed.get("details")?.asString ?: catString

                                val category = try {
                                    ScrapCategory.valueOf(catString)
                                } catch (e: Exception) {
                                    ScrapCategory.OTHER
                                }

                                val finalSubCat = if (subCatString.isNotBlank()) subCatString else (category.subCategories.firstOrNull() ?: "")

                                return@withContext ClassificationResult(
                                    category = category,
                                    subCategory = finalSubCat,
                                    confidence = conf,
                                    rawLabels = listOf("Gemini AI ($modelName): $detail")
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun mapLabelsToCategory(labels: List<String>): ScrapCategory {
        for (label in labels) {
            when {
                label.contains("metal") || label.contains("steel") || label.contains("iron") || label.contains("aluminum") || label.contains("copper") || label.contains("tin") || label.contains("can") || label.contains("wire") || label.contains("pipe") || label.contains("hardware") || label.contains("tool") || label.contains("chain") || label.contains("nail") || label.contains("screw") -> return ScrapCategory.METAL
                label.contains("plastic") || label.contains("bottle") || label.contains("pvc") || label.contains("polymer") || label.contains("container") || label.contains("cup") || label.contains("toy") || label.contains("packaging") || label.contains("bucket") || label.contains("tub") -> return ScrapCategory.PLASTIC
                label.contains("wood") || label.contains("lumber") || label.contains("timber") || label.contains("pallet") || label.contains("board") || label.contains("table") || label.contains("chair") || label.contains("furniture") || label.contains("crate") -> return ScrapCategory.WOOD
                label.contains("paper") || label.contains("cardboard") || label.contains("box") || label.contains("carton") || label.contains("newspaper") || label.contains("book") || label.contains("document") || label.contains("envelope") -> return ScrapCategory.PAPER
                label.contains("glass") || label.contains("window") || label.contains("jar") || label.contains("dishware") || label.contains("wine bottle") || label.contains("beer bottle") -> return ScrapCategory.GLASS
                label.contains("electronic") || label.contains("computer") || label.contains("phone") || label.contains("circuit") || label.contains("battery") || label.contains("appliance") || label.contains("cable") || label.contains("gadget") || label.contains("screen") || label.contains("monitor") || label.contains("keyboard") || label.contains("mouse") -> return ScrapCategory.EWASTE
                label.contains("oil") || label.contains("chemical") || label.contains("paint") || label.contains("solvent") || label.contains("toxic") || label.contains("liquid") -> return ScrapCategory.CHEMICAL
                label.contains("rubber") || label.contains("tire") || label.contains("latex") || label.contains("wheel") -> return ScrapCategory.RUBBER
            }
        }
        return ScrapCategory.OTHER
    }
}





