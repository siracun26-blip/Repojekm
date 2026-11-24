package dev.nxnative.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nxnative.launcher.model.GameProfile
import dev.nxnative.launcher.ui.LauncherViewModel
import dev.nxnative.launcher.ui.NXNativeAppScreen
import dev.nxnative.launcher.ui.theme.NXNativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NXNativeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val vm: LauncherViewModel = viewModel()
                    val uiState by vm.uiState.collectAsStateWithLifecycle()

                    val safFolderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            vm.onDumpFolderSelected(uri)
                            val label = vm.uiState.value.dumpFolderDisplayName ?: uri.toString()
                            Toast.makeText(
                                context,
                                context.getString(R.string.storage_folder_granted, label),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            vm.onDumpFolderCancelled()
                            Toast.makeText(
                                context,
                                context.getString(R.string.storage_folder_cancelled),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ ->
                        // Foreground preparation runs regardless; notification is optional
                    }

                    val handlePrepareGame: (GameProfile) -> Unit = { game ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        vm.prepareGame(game)
                    }

                    NXNativeAppScreen(
                        uiState = uiState,
                        onSelectTab = vm::selectTab,
                        onRescanLibrary = vm::rescanLibrary,
                        onOpenSheet = vm::openGameSheet,
                        onDismissSheet = vm::dismissGameSheet,
                        onPrepareGame = handlePrepareGame,
                        onPlayGame = { game ->
                            val intent = Intent(context, GameSurfaceActivity::class.java).apply {
                                putExtra(GameSurfaceActivity.EXTRA_TITLE_ID, game.titleId)
                                putExtra(GameSurfaceActivity.EXTRA_GAME_NAME, game.name)
                                putExtra(GameSurfaceActivity.EXTRA_TARGET_FPS, game.targetFps)
                                putExtra(GameSurfaceActivity.EXTRA_RESOLUTION, game.defaultResolution)
                                putExtra(GameSurfaceActivity.EXTRA_DRIVER, game.customDriver)
                            }
                            context.startActivity(intent)
                        },
                        onSelectDriver = vm::selectDriver,
                        onClearLogs = vm::clearLogs,
                        onChangeWorkers = vm::setWorkerThreads,
                        onRequestStorageAccess = { safFolderLauncher.launch(null) },
                        onBackupSave = vm::backupSaves,
                        onDeletePack = vm::deletePack
                    )
                }
            }
        }
    }
}
