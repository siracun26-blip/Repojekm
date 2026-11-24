package dev.nxnative.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nxnative.launcher.R
import dev.nxnative.launcher.model.GameProfile
import dev.nxnative.launcher.model.GpuDriverEntry
import dev.nxnative.launcher.model.PackStatus
import dev.nxnative.launcher.ui.theme.BadgeModsBg
import dev.nxnative.launcher.ui.theme.BadgeModsFg
import dev.nxnative.launcher.ui.theme.BadgeNeutralBg
import dev.nxnative.launcher.ui.theme.BadgeNeutralFg
import dev.nxnative.launcher.ui.theme.BadgePreparingBg
import dev.nxnative.launcher.ui.theme.BadgePreparingFg
import dev.nxnative.launcher.ui.theme.BadgeReadyBg
import dev.nxnative.launcher.ui.theme.BadgeReadyFg
import dev.nxnative.launcher.ui.theme.JetBrainsMonoFontFamily
import dev.nxnative.launcher.ui.theme.NxSurfaceSelected
import dev.nxnative.launcher.ui.theme.NxTerminalBg
import dev.nxnative.launcher.ui.theme.NxTerminalText

@Composable
fun GamesLibraryTab(
    games: List<GameProfile>,
    onOpenSheet: (GameProfile) -> Unit,
    onPrepareGame: (GameProfile) -> Unit,
    onPlayGame: (GameProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("games_library_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(games, key = { it.titleId }) { game ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenSheet(game) }
                    .testTag("game_card_${game.titleId}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${game.titleId} • v${game.version} • ${game.defaultResolution}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(game.packStatus)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (game.packStatus == PackStatus.PREPARING) {
                        val (cur, tot) = game.shaderProgress
                        val progress = if (tot > 0) (cur.toFloat() / tot.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .testTag("prepare_progress_${game.titleId}")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.shader_progress_label,
                                cur,
                                tot,
                                (progress * 100).toInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (game.packStatus == PackStatus.NOT_PREPARED) {
                                Button(
                                    onClick = { onPrepareGame(game) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("prepare_button_${game.titleId}")
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_prepare_pack))
                                }
                            } else {
                                Button(
                                    onClick = { onPlayGame(game) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("play_button_${game.titleId}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_play_fps, game.targetFps))
                                }
                            }
                            OutlinedButton(
                                onClick = { onOpenSheet(game) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("details_button_${game.titleId}")
                            ) {
                                Text(stringResource(R.string.action_details))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: PackStatus) {
    val (bg, fg) = when (status) {
        PackStatus.NOT_PREPARED -> BadgeNeutralBg to BadgeNeutralFg
        PackStatus.PREPARING -> BadgePreparingBg to BadgePreparingFg
        PackStatus.READY -> BadgeReadyBg to BadgeReadyFg
        PackStatus.READY_WITH_MODS -> BadgeModsBg to BadgeModsFg
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GpuDriversTab(
    gpuInfo: String,
    drivers: List<GpuDriverEntry>,
    activeDriverId: String,
    onSelectDriver: (GpuDriverEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.gpu_readiness_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = gpuInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("gpu_readiness_info")
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.gpu_readiness_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(drivers, key = { it.id }) { driver ->
            val selected = driver.id == activeDriverId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) NxSurfaceSelected else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectDriver(driver) }
                    .testTag("driver_card_${driver.id}")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectDriver(driver) },
                        modifier = Modifier.testTag("driver_radio_${driver.id}")
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(driver.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${driver.architecture} • Version ${driver.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildLogsTab(
    logs: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.build_logs_title),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onClear,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_logs_button")
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NxTerminalBg, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .testTag("build_logs_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs) { line ->
                    Text(
                        text = line,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = NxTerminalText
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    workerThreads: Int,
    dumpFolderDisplayName: String?,
    onChangeWorkers: (Int) -> Unit,
    onRequestStorageAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_workers_label, workerThreads),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = workerThreads.toFloat(),
                        onValueChange = { onChangeWorkers(it.toInt().coerceIn(1, 8)) },
                        valueRange = 1f..8f,
                        steps = 6,
                        modifier = Modifier.testTag("workers_slider")
                    )
                    Text(
                        text = stringResource(R.string.settings_workers_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_dump_folder_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dumpFolderDisplayName ?: stringResource(R.string.settings_dump_folder_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRequestStorageAccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("select_dump_folder_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_grant_storage))
                    }
                }
            }
        }
    }
}

@Composable
fun GameDetailSheet(
    game: GameProfile,
    onBackupSave: () -> Unit,
    onDeletePack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(game.name, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.detail_title_id, game.titleId),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = stringResource(R.string.detail_main_build_id, game.mainBuildId),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.detail_sdk_build_id, game.sdkBuildId),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()
        Text(
            text = stringResource(R.string.detail_patches_header),
            style = MaterialTheme.typography.titleSmall
        )
        game.activeMods.forEach { mod ->
            Text(
                text = "• $mod",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onBackupSave,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("backup_saves_button")
            ) {
                Text(stringResource(R.string.action_backup_saves))
            }
            OutlinedButton(
                onClick = onDeletePack,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("delete_pack_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete_pack))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
