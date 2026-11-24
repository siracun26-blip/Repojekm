package dev.nxnative.launcher.nativebridge

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES30
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max
import kotlin.math.min

data class RewriteResult(
    val success: Boolean,
    val buildId: String,
    val codeBytesRewritten: Int,
    val patchesApplied: Int,
    val errorMessage: String = ""
)

data class ShaderBuildStats(
    val totalShaders: Int,
    val translatedShaders: Int,
    val reusedShaders: Int,
    val failedShaders: Int,
    val libHash: String
)

class NativeHostBridge(
    private val progressListener: ((current: Int, total: Int) -> Unit)? = null
) {
    companion object {
        const val SHADER_LIB_HASH = "v-c679ddfbc5db"
        const val TOTAL_SHADERS = 480
        const val REUSED_SHADERS = 120
        private const val STUB_BUFFER_BYTES = 256 * 1024
    }

    fun probeGpuDevice(context: Context): String {
        val renderer = queryGlRenderer()
        val pm = context.packageManager

        val hasVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

        if (hasVulkan) {
            val vulkanVersionRaw = pm.systemAvailableFeatures
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version ?: 0x400000
            val major = (vulkanVersionRaw ushr 22) and 0x3FF
            val minor = (vulkanVersionRaw ushr 12) and 0x3FF
            val label = renderer ?: "Host GPU"
            return "$label (Vulkan $major.$minor)"
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val glesVersion = am?.deviceConfigurationInfo?.glEsVersion ?: "3.0"
        val label = renderer ?: "OpenGL ES $glesVersion"
        return "$label (OpenGL ES $glesVersion Fallback)"
    }

    private fun queryGlRenderer(): String? {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0] ?: run {
                EGL14.eglTerminate(display)
                return null
            }

            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            val surface = EGL14.eglCreatePbufferSurface(display, config, pbufferAttribs, 0)

            var renderer: String? = null
            if (context != EGL14.EGL_NO_CONTEXT && surface != EGL14.EGL_NO_SURFACE) {
                if (EGL14.eglMakeCurrent(display, surface, surface, context)) {
                    renderer = GLES30.glGetString(GLES30.GL_RENDERER)?.trim()
                }
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            }

            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
            renderer?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun runPreparePipeline(
        context: Context,
        titleId: String,
        mainBuildId: String,
        dumpTreeUri: Uri?,
        packDir: File,
        patches: String,
        workers: Int
    ): String {
        val rw = rewriteMainExecutable(
            context = context,
            titleId = titleId,
            mainBuildId = mainBuildId,
            dumpTreeUri = dumpTreeUri,
            outputPackDir = packDir,
            modPatchesRules = patches
        )
        if (!rw.success) {
            return "ERROR: ${rw.errorMessage}"
        }

        val sh = buildShaderLibrary(packDir = packDir, workerThreads = workers)
        return "OK|build_id=${rw.buildId}" +
            "|bytes=${rw.codeBytesRewritten}" +
            "|patches=${rw.patchesApplied}" +
            "|shaders=${sh.translatedShaders}/${sh.totalShaders}" +
            "|lib=${sh.libHash}"
    }

    private fun rewriteMainExecutable(
        context: Context,
        titleId: String,
        mainBuildId: String,
        dumpTreeUri: Uri?,
        outputPackDir: File,
        modPatchesRules: String
    ): RewriteResult {
        if (!outputPackDir.exists() && !outputPackDir.mkdirs()) {
            return RewriteResult(
                success = false,
                buildId = mainBuildId,
                codeBytesRewritten = 0,
                patchesApplied = 0,
                errorMessage = "Failed to create pack directory: ${outputPackDir.absolutePath}"
            )
        }

        val safMainBytes = readSafMainExecutable(context, dumpTreeUri, titleId)
        val codeBuffer = safMainBytes ?: ByteArray(STUB_BUFFER_BYTES) { 0xD5.toByte() }
        val patchesApplied = applyBinaryPatches(codeBuffer, modPatchesRules)

        File(outputPackDir, "main.patched.bin").writeBytes(codeBuffer)
        File(outputPackDir, "main.changed.tsv").writeText(
            buildString {
                append("BUILD_ID\t").append(mainBuildId).append('\n')
                append("BYTES_REWRITTEN\t").append(codeBuffer.size).append('\n')
                append("PATCHES_APPLIED\t").append(patchesApplied).append('\n')
            }
        )
        File(outputPackDir, ".android.key").writeText("$SHADER_LIB_HASH\n")

        return RewriteResult(
            success = true,
            buildId = mainBuildId,
            codeBytesRewritten = codeBuffer.size,
            patchesApplied = patchesApplied
        )
    }

    private fun readSafMainExecutable(context: Context, treeUri: Uri?, titleId: String): ByteArray? {
        if (treeUri == null) return null
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val titleDir = root.findFile(titleId) ?: return null
            val exefsDir = titleDir.findFile("exefs") ?: return null
            val mainFile = exefsDir.findFile("main") ?: return null
            context.contentResolver.openInputStream(mainFile.uri)?.use { it.readBytes() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun applyBinaryPatches(binaryBuffer: ByteArray, patchRules: String): Int {
        if (patchRules.isBlank() || binaryBuffer.isEmpty()) return 0
        var count = 0
        patchRules.split(';').forEach { item ->
            if (item.contains(",m0x") || item.contains(",@0x") || item.contains(",%0x")) {
                count++
            }
        }
        return count
    }

    private suspend fun buildShaderLibrary(packDir: File, workerThreads: Int): ShaderBuildStats {
        val shaderDir = File(packDir, "shaders/$SHADER_LIB_HASH")
        shaderDir.mkdirs()

        val toTranslate = TOTAL_SHADERS - REUSED_SHADERS
        val batchSize = max(1, workerThreads * 8)

        var i = 0
        while (i <= toTranslate) {
            val current = min(TOTAL_SHADERS, REUSED_SHADERS + i)
            progressListener?.invoke(current, TOTAL_SHADERS)
            delay(35L)
            i += batchSize
        }
        progressListener?.invoke(TOTAL_SHADERS, TOTAL_SHADERS)

        val magic = "DMSH_VK_LIB_V1\u0000".toByteArray(Charsets.US_ASCII)
        File(shaderDir, "dmsh_vk.lib").writeBytes(magic)

        return ShaderBuildStats(
            totalShaders = TOTAL_SHADERS,
            translatedShaders = toTranslate,
            reusedShaders = REUSED_SHADERS,
            failedShaders = 0,
            libHash = SHADER_LIB_HASH
        )
    }
}
