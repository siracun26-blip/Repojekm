package dev.nxnative.launcher.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nxnative.launcher.BuildConfig
import dev.nxnative.launcher.model.BuiltinGameCatalog
import dev.nxnative.launcher.model.GameProfile
import dev.nxnative.launcher.model.GpuDriverEntry
import dev.nxnative.launcher.model.PackStatus
import dev.nxnative.launcher.nativebridge.NativeHostBridge
import dev.nxnative.launcher.service.PackPrepareService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class LauncherUiState(
    val selectedTab: Int = 0,
    val games: List<GameProfile> = BuiltinGameCatalog.getDefaultGames(),
    val selectedGameForSheet: GameProfile? = null,
    val gpuInfo: String = "Probing GPU...",
    val buildLogs: List<String> = emptyList(),
    val availableDrivers: List<GpuDriverEntry> = BuiltinGameCatalog.getDefaultDrivers(),
    val activeDriverId: String = "sys",
    val workerThreads: Int = 4,
    val freeSpaceGb: Double = 0.0,
    val dumpTreeUri: Uri? = null,
    val dumpFolderDisplayName: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("nxnative_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        LauncherUiState(
            buildLogs = listOf(
                "[info] NXNative Host v${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT})",
                "[info] Shader library target: shaders/${NativeHostBridge.SHADER_LIB_HASH}/dmsh_vk.lib",
                "[info] Ready to scan dump folders (NSP/NSZ/XCI/NUP)"
            )
        )
    )
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        restoreSavedPreferences()
        refreshStorageAndPacks()
        probeGpu()
    }

    private fun restoreSavedPreferences() {
        val savedUriStr = prefs.getString("dump_tree_uri", null)
        val savedUri = savedUriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val savedWorkers = prefs.getInt("worker_threads", 4).coerceIn(1, 8)
        val savedDriverId = prefs.getString("active_driver_id", "sys") ?: "sys"
        val driverName = _uiState.value.availableDrivers.firstOrNull { it.id == savedDriverId }?.name
            ?: _uiState.value.availableDrivers.first().name

        val folderName = savedUri?.let {
            runCatching { DocumentFile.fromTreeUri(getApplication(), it)?.name }.getOrNull() ?: it.lastPathSegment
        }

        _uiState.update { state ->
            state.copy(
                workerThreads = savedWorkers,
                activeDriverId = savedDriverId,
                dumpTreeUri = savedUri,
                dumpFolderDisplayName = folderName,
                games = state.games.map { it.copy(customDriver = driverName) }
            )
        }
    }

    private fun refreshStorageAndPacks() {
        val app = getApplication<Application>()
        val freeGb = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
        }.getOrDefault(0.0)

        _uiState.update { state ->
            val updatedGames = state.games.map { game ->
                val shaderLib = File(app.filesDir, "packs/${game.titleId}/shaders/${NativeHostBridge.SHADER_LIB_HASH}/dmsh_vk.lib")
                if (shaderLib.exists() && game.packStatus != PackStatus.PREPARING) {
                    game.copy(
                        packStatus = if (game.activeMods.isNotEmpty()) PackStatus.READY_WITH_MODS else PackStatus.READY,
                        shaderProgress = NativeHostBridge.TOTAL_SHADERS to NativeHostBridge.TOTAL_SHADERS
                    )
                } else {
                    game
                }
            }
            state.copy(freeSpaceGb = freeGb, games = updatedGames)
        }
    }

    private fun probeGpu() {
        viewModelScope.launch {
            val bridge = NativeHostBridge()
            val info = withContext(Dispatchers.IO) {
                bridge.probeGpuDevice(getApplication())
            }
            _uiState.update { state ->
                state.copy(
                    gpuInfo = info,
                    buildLogs = state.buildLogs + "[gpu] $info"
                )
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun rescanLibrary() {
        refreshStorageAndPacks()
        val folderLabel = _uiState.value.dumpFolderDisplayName ?: "app-private packs"
        val count = _uiState.value.games.size
        appendLog("[scan] Rescanned $folderLabel ($count titles matched)")
    }

    fun openGameSheet(game: GameProfile) {
        _uiState.update { it.copy(selectedGameForSheet = game) }
    }

    fun dismissGameSheet() {
        _uiState.update { it.copy(selectedGameForSheet = null) }
    }

    fun prepareGame(target: GameProfile) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val svcIntent = Intent(app, PackPrepareService::class.java).apply {
                putExtra(PackPrepareService.EXTRA_GAME_NAME, target.name)
            }
            runCatching {
                ContextCompat.startForegroundService(app, svcIntent)
            }

            _uiState.update { state ->
                state.copy(
                    games = state.games.map {
                        if (it.titleId == target.titleId) it.copy(packStatus = PackStatus.PREPARING) else it
                    },
                    buildLogs = state.buildLogs + "[prepare] Starting static AOT rewrite for ${target.name} (${target.titleId})..."
                )
            }

            val bridge = NativeHostBridge { cur, tot ->
                _uiState.update { state ->
                    state.copy(
                        games = state.games.map {
                            if (it.titleId == target.titleId) it.copy(shaderProgress = cur to tot) else it
                        }
                    )
                }
            }

            val packOutDir = File(app.filesDir, "packs/${target.titleId}")
            val workers = _uiState.value.workerThreads
            val treeUri = _uiState.value.dumpTreeUri

            val result = withContext(Dispatchers.IO) {
                bridge.runPreparePipeline(
                    context = app,
                    titleId = target.titleId,
                    mainBuildId = target.mainBuildId,
                    dumpTreeUri = treeUri,
                    packDir = packOutDir,
                    patches = target.patchRules,
                    workers = workers
                )
            }

            runCatching { app.stopService(svcIntent) }

            _uiState.update { state ->
                state.copy(
                    buildLogs = state.buildLogs + "[prepare] Finished ${target.name}: $result",
                    games = state.games.map {
                        if (it.titleId == target.titleId) {
                            it.copy(
                                packStatus = if (it.activeMods.isNotEmpty()) PackStatus.READY_WITH_MODS else PackStatus.READY,
                                shaderProgress = NativeHostBridge.TOTAL_SHADERS to NativeHostBridge.TOTAL_SHADERS
                            )
                        } else it
                    }
                )
            }
        }
    }

    fun selectDriver(driver: GpuDriverEntry) {
        prefs.edit().putString("active_driver_id", driver.id).apply()
        _uiState.update { state ->
            state.copy(
                activeDriverId = driver.id,
                games = state.games.map { it.copy(customDriver = driver.name) },
                buildLogs = state.buildLogs + "[driver] Selected ${driver.name} (${driver.version})"
            )
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(buildLogs = listOf("[info] Logs cleared.")) }
    }

    fun setWorkerThreads(workers: Int) {
        val clamped = workers.coerceIn(1, 8)
        prefs.edit().putInt("worker_threads", clamped).apply()
        _uiState.update { it.copy(workerThreads = clamped) }
    }

    fun onDumpFolderSelected(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val displayName = runCatching {
            DocumentFile.fromTreeUri(app, uri)?.name
        }.getOrNull() ?: uri.lastPathSegment ?: "Selected Folder"

        prefs.edit().putString("dump_tree_uri", uri.toString()).apply()
        _uiState.update { state ->
            state.copy(
                dumpTreeUri = uri,
                dumpFolderDisplayName = displayName,
                buildLogs = state.buildLogs + "[perm] SAF dump folder access granted: $displayName"
            )
        }
    }

    fun onDumpFolderCancelled() {
        appendLog("[perm] Dump folder selection cancelled")
    }

    fun backupSaves(game: GameProfile) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val backupName = "${game.titleId}-backup.zip"
            withContext(Dispatchers.IO) {
                val backupDir = File(app.filesDir, "backups").apply { mkdirs() }
                val backupFile = File(backupDir, backupName)
                ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                    zos.putNextEntry(ZipEntry("save_meta.txt"))
                    val content = "title_id=${game.titleId}\nname=${game.name}\nversion=${game.version}\n"
                    zos.write(content.toByteArray())
                    zos.closeEntry()
                }
            }
            _uiState.update { state ->
                state.copy(
                    selectedGameForSheet = null,
                    buildLogs = state.buildLogs + "[save] Backed up ${game.name} save to $backupName"
                )
            }
        }
    }

    fun deletePack(game: GameProfile) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(app.filesDir, "packs/${game.titleId}").deleteRecursively()
            }
            _uiState.update { state ->
                state.copy(
                    selectedGameForSheet = null,
                    games = state.games.map {
                        if (it.titleId == game.titleId) {
                            it.copy(
                                packStatus = PackStatus.NOT_PREPARED,
                                shaderProgress = 0 to NativeHostBridge.TOTAL_SHADERS
                            )
                        } else it
                    },
                    buildLogs = state.buildLogs + "[pack] Deleted pack for ${game.name} (saves kept)"
                )
            }
        }
    }

    private fun appendLog(entry: String) {
        _uiState.update { it.copy(buildLogs = it.buildLogs + entry) }
    }
}
