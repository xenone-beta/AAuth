package dev.aauth.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.aauth.app.ui.about.AboutScreen
import dev.aauth.app.ui.add.ManualEntryScreen
import dev.aauth.app.ui.home.HomeScreen
import dev.aauth.app.ui.scan.ScanScreen
import dev.aauth.app.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun AAuthApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(context.getString(message.resId, *message.args.toTypedArray()))
        }
    }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val secureScreen by viewModel.secureScreen.collectAsStateWithLifecycle()
    val pinLength by viewModel.pinLength.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                accounts = accounts,
                snackbarHostState = snackbarHostState,
                onScan = { navController.navigate(Routes.SCAN) },
                onManual = { navController.navigate(Routes.MANUAL) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onDelete = { id -> viewModel.delete(id) },
                onEdit = { id, issuer, label -> viewModel.rename(id, issuer, label) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onQrCode = { raw ->
                    viewModel.onQrScanned(raw)
                    navController.popBackStack()
                },
                onManual = {
                    navController.popBackStack()
                    navController.navigate(Routes.MANUAL)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MANUAL) {
            ManualEntryScreen(
                onSave = { account ->
                    viewModel.addManual(account)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                appLock = appLock,
                secureScreen = secureScreen,
                pinLength = pinLength,
                snackbarHostState = snackbarHostState,
                onAppLockChange = { viewModel.setAppLock(it) },
                onSecureScreenChange = { viewModel.setSecureScreen(it) },
                onSavePin = { viewModel.savePin(it) },
                onRemovePin = { viewModel.removePin() },
                onExport = { uri, password -> viewModel.exportBackup(uri, password) },
                onImport = { uri, password -> viewModel.importBackup(uri, password) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
