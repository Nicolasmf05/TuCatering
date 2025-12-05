package com.example.catering.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.catering.ui.screens.auth.LoginScreen
import com.example.catering.ui.screens.auth.RegisterScreen
import com.example.catering.ui.screens.auth.WelcomeScreen
import com.example.catering.ui.screens.auth.AuthViewModel
import com.example.catering.ui.screens.home.PantallaInicio

@Composable
fun NavGraph(nav: NavHostController, authViewModel: AuthViewModel) {
    val authState by authViewModel.uiState.collectAsState()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(authState.currentUser, currentRoute) {
        val user = authState.currentUser
        if (user != null && currentRoute != Destino.Inicio.ruta) {
            nav.navigate(Destino.Inicio.ruta) {
                popUpTo(Destino.Bienvenida.ruta) { inclusive = true }
                launchSingleTop = true
            }
        } else if (user == null && currentRoute == Destino.Inicio.ruta) {
            nav.navigate(Destino.Bienvenida.ruta) {
                popUpTo(Destino.Bienvenida.ruta) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Destino.Bienvenida.ruta) {
        composable(Destino.Bienvenida.ruta) {
            WelcomeScreen(
                onCreateAccountClick = { nav.navigate(Destino.Registro.ruta) },
                onLoginClick = { nav.navigate(Destino.InicioSesion.ruta) }
            )
        }
        composable(Destino.InicioSesion.ruta) {
            LoginScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onGoToRegister = {
                    nav.navigate(Destino.Registro.ruta) { launchSingleTop = true }
                },
                onBack = { nav.popBackStack() },
                onClearError = authViewModel::clearError
            )
        }
        composable(Destino.Registro.ruta) {
            RegisterScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onRegister = { data -> authViewModel.register(data) },
                onBack = { nav.popBackStack() },
                onClearError = authViewModel::clearError
            )
        }
        composable(Destino.Inicio.ruta) {
            val user = authState.currentUser
            if (user != null) {
                PantallaInicio(
                    user = user,
                    onLogout = { authViewModel.logout() }
                )
            }
        }
    }
}
