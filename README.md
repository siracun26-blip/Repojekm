# NXNative Launcher

Android application built with Kotlin, Jetpack Compose, and Material Design 3 (targeting Android SDK 36, minimum SDK 24).

## Features
- **Games Library**: Manage game profiles (`GameProfile`), inspect Title ID / Build ID metadata, prepare AOT binary packs and translated shader libraries (`dmsh_vk.lib`) with live progress tracking and foreground service notifications, back up saves to ZIP archives, and delete cached packs.
- **GPU & Drivers**: Probes host GPU Vulkan and OpenGL ES 3.0 capabilities via EGL/PackageManager and lets users switch active driver profiles.
- **Build & Runtime Digest**: Live terminal-style log viewer tracking GPU probe results, SAF dump folder selection, pack rewrite statistics, and save backups.
- **Settings**: Configure shader translator worker thread concurrency (1–8 workers) and grant dump folder access via Storage Access Framework (`OpenDocumentTree`).
- **In-Game Runtime Surface**: Landscape OpenGL ES 3.0 `GLSurfaceView` with a toggleable performance HUD, in-game control panel, and FPS cap selector (`30` / `60` FPS).