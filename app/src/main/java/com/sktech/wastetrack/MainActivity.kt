package com.sktech.wastetrack

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.ui.navigation.BottomNavBar
import com.sktech.wastetrack.ui.navigation.NavGraph
import com.sktech.wastetrack.ui.navigation.Screen
import com.sktech.wastetrack.ui.navigation.bottomNavItems
import com.sktech.wastetrack.ui.theme.WastetrackTheme
import com.sktech.wastetrack.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: IAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by LocaleHelper.currentLanguage.collectAsStateWithLifecycle()
            val configuration = LocalConfiguration.current

            val localizedConfig = remember(currentLanguage, configuration) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                Configuration(configuration).apply {
                    setLocale(locale)
                }
            }

            LaunchedEffect(currentLanguage) {
                LocaleHelper.updateResourcesLocale(this@MainActivity, currentLanguage)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig
            ) {
                key(currentLanguage) {
                    WastetrackTheme {
                        val navController = androidx.navigation.compose.rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        
                        val currentUser by produceState<com.sktech.wastetrack.domain.model.User?>(initialValue = null, keys = arrayOf(currentRoute, currentLanguage)) {
                            value = authRepository.getCurrentUser()
                        }

                        val startDestination = if (authRepository.isLoggedIn()) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Login.route
                        }

                        val filteredNavItems = remember(currentUser) {
                            if (currentUser?.role == UserRole.RECYCLER) {
                                bottomNavItems.filter { it.route != Screen.ScrapLog.route }
                            } else {
                                bottomNavItems
                            }
                        }

                        val showBottomNav = currentRoute in filteredNavItems.map { it.route }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (showBottomNav) {
                                    BottomNavBar(
                                        currentRoute = currentRoute,
                                        items = filteredNavItems,
                                        onNavigate = { route ->
                                            navController.navigate(route) {
                                                popUpTo(Screen.Dashboard.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            NavGraph(
                                navController = navController,
                                innerPadding = innerPadding,
                                startDestination = startDestination,
                                authRepository = authRepository
                            )
                        }
                    }
                }
            }
        }
    }
}