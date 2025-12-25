package com.bluemix.clients_lead.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bluemix.clients_lead.features.Clients.presentation.ClientDetailScreen
import com.bluemix.clients_lead.features.Clients.presentation.ClientsScreen
import com.bluemix.clients_lead.features.Clients.presentation.CreateClientScreen
import com.bluemix.clients_lead.features.auth.presentation.screens.AuthScreen
import com.bluemix.clients_lead.features.auth.vm.SessionViewModel
import com.bluemix.clients_lead.features.map.presentation.MapScreen
import com.bluemix.clients_lead.features.settings.presentation.ProfileScreen
import com.bluemix.clients_lead.features.timesheet.presentation.ActivityScreen
import org.koin.androidx.compose.koinViewModel
import com.bluemix.clients_lead.features.*

/**
 * Main app navigation graph with gate pattern for auth routing.
 *
 * Flow:
 * 1. Gate screen checks authentication status
 * 2. Routes to Auth or Map based on session state
 * 3. Protected screens verify auth status
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navigationManager = remember(navController) {
        NavigationManager(navController)
    }

    val sessionVM: SessionViewModel = koinViewModel()
    val session by sessionVM.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Route.Gate,
        enterTransition = { fadeIn() + slideInHorizontally { it } },
        exitTransition = { fadeOut() + slideOutHorizontally { -it } },
        popEnterTransition = { fadeIn() + slideInHorizontally { -it } },
        popExitTransition = { fadeOut() + slideOutHorizontally { it } }
    ) {
        // -------- Gate/Splash Screen --------
        composable<Route.Gate> {
            SplashScreen(isReady = session.isReady)

            LaunchedEffect(session) {
                if (!session.isReady) return@LaunchedEffect   // Wait until restore completed

                if (session.isAuthenticated) {
                    navigationManager.navigateToMain()
                } else {
                    navigationManager.navigateToAuth()
                }
            }

        }

        // -------- Authentication --------
        composable<Route.Auth> {
            AuthScreen(
                onSignedIn = {
                    navigationManager.navigateToMain()
                }
            )
        }

        // -------- Protected Tab Screens --------
        composable<Route.Map> {
            ProtectedRoute(
                isAuthenticated = session.isAuthenticated,
                onNavigateToAuth = navigationManager::navigateToAuth
            ) {
                MainScaffold(
                    currentRoute = Route.Map,
                    navigationManager = navigationManager
                ) {
                    MapScreen(
                        onNavigateToClientDetail = { clientId ->
                            navigationManager.navigateToClientDetail(clientId)
                        }
                    )
                }
            }
        }

        composable<Route.Clients> {
            ProtectedRoute(
                isAuthenticated = session.isAuthenticated,
                onNavigateToAuth = navigationManager::navigateToAuth
            ) {
                MainScaffold(
                    currentRoute = Route.Clients,
                    navigationManager = navigationManager
                ) {
                    ClientsScreen(
                        onNavigateToDetail = { clientId ->
                            navigationManager.navigateToClientDetail(clientId)
                        },
                        onNavigateToCreateClient = {
                            navigationManager.navigateToCreateClient()
                        }
                    )
                }
            }
        }

        composable<Route.Activity> {
            ProtectedRoute(
                isAuthenticated = session.isAuthenticated,
                onNavigateToAuth = navigationManager::navigateToAuth
            ) {
                MainScaffold(
                    currentRoute = Route.Activity,
                    navigationManager = navigationManager
                ) {
                    ActivityScreen()
                }
            }
        }

        composable<Route.Profile> {
            ProtectedRoute(
                isAuthenticated = session.isAuthenticated,
                onNavigateToAuth = navigationManager::navigateToAuth
            ) {
                MainScaffold(
                    currentRoute = Route.Profile,
                    navigationManager = navigationManager
                ) {
                    ProfileScreen(
                        onNavigateToAuth = navigationManager::navigateToAuth
                    )
                }
            }
        }

        composable<Route.CreateClient> {
            CreateClientScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // -------- Detail Screen (No Bottom Bar) --------
        composable<Route.ClientDetail> { backStack ->
            val args = backStack.toRoute<Route.ClientDetail>()

            ProtectedRoute(
                isAuthenticated = session.isAuthenticated,
                onNavigateToAuth = navigationManager::navigateToAuth
            ) {
                ClientDetailScreen(
                    clientId = args.clientId,
                    onNavigateBack = navigationManager::navigateBack
                )
            }
        }
    }
}

/**
 * Wrapper to protect routes from unauthenticated access
 */
@Composable
private fun ProtectedRoute(
    isAuthenticated: Boolean,
    onNavigateToAuth: () -> Unit,
    content: @Composable () -> Unit
) {
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            onNavigateToAuth()
        }
    }

    if (isAuthenticated) {
        content()
    }
}

//@Composable
//private fun SplashScreen(isReady: Boolean) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(AppTheme.colors.background),
//        contentAlignment = Alignment.Center
//    ) {
//        AnimatedVisibility(
//            visible = !isReady,
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
//            Column(
//                modifier = Modifier
//                    .wrapContentWidth()
//                    .wrapContentHeight(),
//                verticalArrangement = Arrangement.spacedBy(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "Clients Lead",
//                    style = AppTheme.typography.h2,
//                    color = AppTheme.colors.text
//                )
//                CircularProgressIndicator(
//                    modifier = Modifier.size(32.dp),
//                    color = AppTheme.colors.primary
//                )
//            }
//        }
//    }
//}
