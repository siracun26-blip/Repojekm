package dev.nxnative.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nxnative.launcher.R
import dev.nxnative.launcher.model.GameProfile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NXNativeAppScreen(
    uiState: LauncherUiState,
    onSelectTab: (Int) -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenSheet: (GameProfile) -> Unit,
    onDismissSheet: () -> Unit,
    onPrepareGame: (GameProfile) -> Unit,
    onPlayGame: (GameProfile) -> Unit,
    onSelectDriver: (dev.nxnative.launcher.model.GpuDriverEntry) -> Unit,
    onClearLogs: () -> Unit,
    onChangeWorkers: (Int) -> Unit,
    onRequestStorageAccess: () -> Unit,
    onBackupSave: (GameProfile) -> Unit,
    onDeletePack: (GameProfile) -> Unit
) {
    val tabItems = listOf(
        Triple(0, stringResource(R.string.tab_games), Icons.Default.SportsEsports),
        Triple(1, stringResource(R.string.tab_gpu_drivers), Icons.Default.Memory),
        Triple(2, stringResource(R.string.tab_build_logs), Icons.Default.Terminal),
        Triple(3, stringResource(R.string.tab_settings), Icons.Default.Settings)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.host_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Free: %.1f GiB • %s",
                                    uiState.freeSpaceGb,
                                    uiState.gpuInfo
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onRescanLibrary,
                            modifier = Modifier.testTag("rescan_library_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_rescan_library)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        tabItems.forEach { (idx, label, icon) ->
                            NavigationBarItem(
                                selected = uiState.selectedTab == idx,
                                onClick = { onSelectTab(idx) },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_$idx")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        tabItems.forEach { (idx, label, icon) ->
                            NavigationRailItem(
                                selected = uiState.selectedTab == idx,
                                onClick = { onSelectTab(idx) },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_$idx")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (uiState.selectedTab) {
                        0 -> GamesLibraryTab(
                            games = uiState.games,
                            onOpenSheet = onOpenSheet,
                            onPrepareGame = onPrepareGame,
                            onPlayGame = onPlayGame
                        )

                        1 -> GpuDriversTab(
                            gpuInfo = uiState.gpuInfo,
                            drivers = uiState.availableDrivers,
                            activeDriverId = uiState.activeDriverId,
                            onSelectDriver = onSelectDriver
                        )

                        2 -> BuildLogsTab(
                            logs = uiState.buildLogs,
                            onClear = onClearLogs
                        )

                        3 -> SettingsTab(
                            workerThreads = uiState.workerThreads,
                            dumpFolderDisplayName = uiState.dumpFolderDisplayName,
                            onChangeWorkers = onChangeWorkers,
                            onRequestStorageAccess = onRequestStorageAccess
                        )
                    }
                }
            }
        }

        uiState.selectedGameForSheet?.let { game ->
            ModalBottomSheet(onDismissRequest = onDismissSheet) {
                GameDetailSheet(
                    game = game,
                    onBackupSave = { onBackupSave(game) },
                    onDeletePack = { onDeletePack(game) }
                )
            }
        }
    }
}
