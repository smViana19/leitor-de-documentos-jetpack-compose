package com.example.leitordocumento_compose.presentation.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.documentscan.DocumentScanScreen
import com.example.leitordocumento_compose.data.OcrResultado
import com.example.leitordocumento_compose.presentation.ui.screen.FormScreen
import com.example.leitordocumento_compose.presentation.ui.screen.FormularioCnhScreen
import com.example.leitordocumento_compose.presentation.ui.screen.FormularioCrlvScreen
import com.example.leitordocumento_compose.presentation.ui.screen.FormularioPlacaScreen
import com.example.leitordocumento_compose.presentation.ui.screen.FormularioRgScreen
import com.example.leitordocumento_compose.presentation.ui.screen.HistoricoScreen
import com.example.leitordocumento_compose.presentation.ui.screen.HomeScreen
import com.example.leitordocumento_compose.presentation.ui.screen.PlacaScanScreen
import com.example.leitordocumento_compose.presentation.ui.screen.SettingScreen
import com.example.leitordocumento_compose.utils.OcrResultadoHolder

fun NavController.navegarParaFormulario(resultado: OcrResultado, id: Long) {
    OcrResultadoHolder.set(resultado)
    val rota = when (resultado) {
        is OcrResultado.Cnh        -> "${Screens.TELA_FORMULARIO_CNH.route}/$id"
        is OcrResultado.Rg         -> "${Screens.TELA_FORMULARIO_RG.route}/$id"
        is OcrResultado.Placa      -> "${Screens.TELA_FORMULARIO_PLACA.route}/$id"
        is OcrResultado.Crlv       -> "${Screens.TELA_FORMULARIO_CRLV.route}/$id"
        is OcrResultado.Desconhecido -> "${Screens.TELA_FORMULARIO_CNH.route}/$id"
    }
    navigate(rota)
}

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
            composable(route = Screens.TELA_HOME.route) {
                HomeScreen(navController = navController)
            }
            composable(route = Screens.TELA_SCANNER.route) {
                DocumentScanScreen(navController = navController)
            }

            composable(
                "${Screens.TELA_FORMULARIO_CNH.route}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                val id = back.arguments!!.getLong("id")
                val dados = (OcrResultadoHolder.consume() as? OcrResultado.Cnh)?.dadosCNH
                FormularioCnhScreen(
                    id = id,
                    dados = null,
                    onConfirm = { navController.popBackStack(Screens.TELA_HOME.route, false) },
                    onReler = { navController.popBackStack() }
                )
            }

            composable(
                "${Screens.TELA_FORMULARIO_RG.route}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                val id = back.arguments!!.getLong("id")

//                val dados = (OcrResultadoHolder.consume() as? OcrResultado.Rg)?.dadosRG
                FormularioRgScreen(
                    id = id,
                    dados = null,
                    onConfirm = { navController.popBackStack(Screens.TELA_HOME.route, false) },
                    onReler = { navController.popBackStack() }
                )
            }

            composable(
                "${Screens.TELA_FORMULARIO_PLACA.route}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                val id = back.arguments!!.getLong("id")
                FormularioPlacaScreen(
                    id = id,
                    dados = null,
                    onReler = { navController.popBackStack() }
                )
            }

            composable(
                "${Screens.TELA_FORMULARIO_CRLV.route}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                val id = back.arguments!!.getLong("id")
                FormularioCrlvScreen(
                    id = id,
                    dados = null,
                    onConfirm = { navController.popBackStack(Screens.TELA_HOME.route, false) },
                    onReler = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screens.TELA_SCANNER_PLACA.route}/{tipoVeiculo}",
                arguments = listOf(navArgument("tipoVeiculo") { type = NavType.StringType })
            ) { backStackEntry ->
                val tipoStr = backStackEntry.arguments?.getString("tipoVeiculo") ?: "CARRO"
                val tipoVeiculo = TipoVeiculo.valueOf(tipoStr)

                PlacaScanScreen(
                    navController = navController,
                    tipoVeiculo = tipoVeiculo
                )
            }

            composable(route = Screens.TELA_CONFIGURACAO.route) {
                SettingScreen()
            }
            composable(route = Screens.TELA_FORMULARIO.route) {
                FormScreen(navController = navController)
            }
            composable(route = Screens.TELA_HISTORICO.route) {
                HistoricoScreen(navController = navController)
            }

        }

    })
}