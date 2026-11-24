package dev.nxnative.launcher.model

enum class PackStatus(val label: String) {
    NOT_PREPARED("Needs Prepare"),
    PREPARING("Preparing Pack..."),
    READY("Ready to Play"),
    READY_WITH_MODS("Ready to Play (Mods)")
}

data class GameProfile(
    val titleId: String,
    val name: String,
    val version: String,
    val mainBuildId: String,
    val sdkBuildId: String,
    val defaultResolution: String,
    val targetFps: Int,
    val patchRules: String,
    val dumpPath: String = "Switch2/dumps/",
    val packStatus: PackStatus = PackStatus.NOT_PREPARED,
    val shaderProgress: Pair<Int, Int> = 0 to 480,
    val activeMods: List<String> = emptyList(),
    val customDriver: String = "System GPU Driver (Vulkan / GLES 3.0)",
    val playTimeMinutes: Int = 0
)

data class GpuDriverEntry(
    val id: String,
    val name: String,
    val architecture: String,
    val version: String,
    val hasPlacedMemoryMaps: Boolean = true
)

object BuiltinGameCatalog {
    fun getDefaultGames(): List<GameProfile> = listOf(
        GameProfile(
            titleId = "0100000000010000",
            name = "Super Mario Odyssey",
            version = "1.4.1",
            mainBuildId = "b424be150a8e7d78701cbe7a439d9ebf",
            sdkBuildId = "fcf93a41b08c5193a1410d262602dc62",
            defaultResolution = "1280x720 (Native)",
            targetFps = 60,
            patchRules = "b424be150a8e7d78701cbe7a439d9ebf,ratio_f32,m0xe9ad94,398ee33f",
            activeMods = listOf("Dynamic 60 FPS", "16:9 Aspect Fix")
        ),
        GameProfile(
            titleId = "010015100B514000",
            name = "Super Mario Bros. Wonder",
            version = "1.2.1",
            mainBuildId = "ff773e90972d544eb79406eaa65396d5",
            sdkBuildId = "e4bd58cac28cf13a08fb417aa013cdcb",
            defaultResolution = "1920x1080 (Docked)",
            targetFps = 60,
            patchRules = "ff773e90972d544eb79406eaa65396d5,time_step_pair_f32,%0x0,1,60:0.066666667;ff773e90972d544eb79406eaa65396d5,width_movz,m0x69b190,08f080d2",
            activeMods = listOf("Docked 1080p", "Fixed Timestep 60Hz")
        ),
        GameProfile(
            titleId = "0100152000022000",
            name = "Mario Kart 8 Deluxe",
            version = "4.0.1",
            mainBuildId = "158c0c7399aabd7bf83795c2ef3484c7",
            sdkBuildId = "ba520906609c503227871a314a94fb5f",
            defaultResolution = "1280x720 (Native)",
            targetFps = 60,
            patchRules = "2c336a9bcf79c3040ce506cdd391b578,area_width_u32,m0xf74514,00050000;2c336a9bcf79c3040ce506cdd391b578,area_height_u32,m0xf74518,d0020000",
            activeMods = listOf("Viewport 1280x720")
        ),
        GameProfile(
            titleId = "01001F5010DFA000",
            name = "Pokémon Legends: Arceus",
            version = "1.1.1",
            mainBuildId = "aee8f150dda1b5a838806e1a5ea6827a",
            sdkBuildId = "b39add63421be525d352e576dab438d3",
            defaultResolution = "1280x720 (Native)",
            targetFps = 30,
            patchRules = "aee8f150dda1b5a838806e1a5ea6827a,inverse_f32,%0x18,1,1:1",
            activeMods = listOf("Inverse Frame Pacing")
        )
    )

    fun getDefaultDrivers(): List<GpuDriverEntry> = listOf(
        GpuDriverEntry("sys", "System GPU Driver (Vulkan / GLES 3.0)", "Auto", "Android Native", true),
        GpuDriverEntry("turnip_25", "Mesa Turnip Adreno v25.0.0", "Adreno 6xx/7xx", "25.0.0", true),
        GpuDriverEntry("panvk_dev", "PanVK Mali Placed-Map", "Mali / Immortalis", "2026.10", true)
    )
}
