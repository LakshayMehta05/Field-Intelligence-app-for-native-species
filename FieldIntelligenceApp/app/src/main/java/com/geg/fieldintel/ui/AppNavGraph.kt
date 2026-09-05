package com.geg.fieldintel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geg.fieldintel.data.model.ScanResult
import com.geg.fieldintel.data.model.Species
import com.geg.fieldintel.ui.arresult.ARResultScreen
import com.geg.fieldintel.ui.chat.ChatScreen
import com.geg.fieldintel.ui.scanner.ScannerScreen

private object Routes {
    const val SCANNER = "scanner"
    const val AR_RESULT = "ar_result"
    const val CHAT = "chat"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    // Holds the last scan result / species so it can be threaded through nav without
    // serializing complex objects through nav args.
    var lastResult = remember { mutableStateOf<ScanResult?>(null) }
    var chatSpecies = remember { mutableStateOf<Species?>(null) }

    NavHost(navController = navController, startDestination = Routes.SCANNER) {

        composable(Routes.SCANNER) {
            ScannerScreen(
                onSpeciesIdentified = { result ->
                    lastResult.value = result
                    navController.navigate(Routes.AR_RESULT)
                },
                onOpenChat = {
                    chatSpecies.value = null
                    navController.navigate(Routes.CHAT)
                }
            )
        }

        composable(Routes.AR_RESULT) {
            val result = lastResult.value
            if (result != null) {
                ARResultScreen(
                    result = result,
                    onBack = { navController.popBackStack() },
                    onAskAboutSpecies = { species ->
                        chatSpecies.value = species
                        navController.navigate(Routes.CHAT)
                    }
                )
            }
        }

        composable(Routes.CHAT) {
            ChatScreen(
                speciesId = chatSpecies.value?.id,
                speciesLabel = chatSpecies.value?.commonName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
