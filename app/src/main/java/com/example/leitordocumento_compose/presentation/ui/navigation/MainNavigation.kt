package com.example.leitordocumento_compose.presentation.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.documentscan.DocumentScanScreen
import com.example.leitordocumento_compose.presentation.ui.screen.FormScreen
import com.example.leitordocumento_compose.presentation.ui.screen.HomeScreen
import com.example.leitordocumento_compose.presentation.ui.screen.PlacaScanScreen
import com.example.leitordocumento_compose.presentation.ui.screen.SettingScreen



@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainNavigation(inicioNavegacao: String) {
    val navController = rememberNavController()

    Scaffold(content = {
        NavHost(
            navController = navController,
            startDestination = inicioNavegacao,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            composable(route = Screens.TELA_HOME.name) {
                HomeScreen(navController = navController)
            }
            composable(route = Screens.TELA_SCANNER.name) {
                DocumentScanScreen(navController = navController)
            }
            composable(route = Screens.TELA_FORMULARIO.name) {
                FormScreen(navController = navController)
            }
            composable(route = Screens.TELA_CONFIGURACAO.name) {
                SettingScreen()
            }
            composable(
                route = "${Screens.TELA_SCANNER_PLACA.name}/{tipoVeiculo}",
                arguments = listOf(navArgument("tipoVeiculo") { type = NavType.StringType })
            ) { backStackEntry ->
                val tipoStr = backStackEntry.arguments?.getString("tipoVeiculo") ?: "CARRO"
                val tipoVeiculo = TipoVeiculo.valueOf(tipoStr)

                PlacaScanScreen(
                    navController = navController,
                    tipoVeiculo = tipoVeiculo
                )
            }

        }

    })
}