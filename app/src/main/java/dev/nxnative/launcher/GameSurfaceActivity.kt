package dev.nxnative.launcher

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nxnative.launcher.ui.theme.JetBrainsMonoFontFamily
import dev.nxnative.launcher.ui.theme.NXNativeTheme
import dev.nxnative.launcher.ui.theme.NxHudBg
import dev.nxnative.launcher.ui.theme.NxIconBg
import dev.nxnative.launcher.ui.theme.NxPanelBg
import dev.nxnative.launcher.ui.theme.NxStopButton
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class GameSurfaceActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TITLE_ID = "title_id"
        const val EXTRA_GAME_NAME = "game_name"
        const val EXTRA_TARGET_FPS = "target_fps"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_DRIVER = "driver"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val gameName = intent.getStringExtra(EXTRA_GAME_NAME) ?: "Unknown Title"
        val titleId = intent.getStringExtra(EXTRA_TITLE_ID) ?: "0000000000000000"
        val targetFps = intent.getIntExtra(EXTRA_TARGET_FPS, 60)
        val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1280x720"
        val driver = intent.getStringExtra(EXTRA_DRIVER) ?: "System GPU Driver"

        setContent {
            NXNativeTheme {
                InGameRuntimeScreen(
                    gameName = gameName,
                    titleId = titleId,
                    targetFps = targetFps,
                    resolution = resolution,
                    driver = driver,
                    onStopGame = { finish() }
                )
            }
        }
    }
}

@Composable
fun InGameRuntimeScreen(
    gameName: String,
    titleId: String,
    targetFps: Int,
    resolution: String,
    driver: String,
    onStopGame: () -> Unit
) {
    var panelOpen by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(true) }
    var fpsCap by remember { mutableIntStateOf(targetFps) }

    BackHandler(enabled = panelOpen) {
        panelOpen = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(object : GLSurfaceView.Renderer {
                        private var tick = 0f
                        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                            GLES30.glClearColor(0.06f, 0.09f, 0.14f, 1.0f)
                        }

                        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                            GLES30.glViewport(0, 0, width, height)
                        }

                        override fun onDrawFrame(gl: GL10?) {
                            tick += 0.03f
                            val pulse = 0.08f + 0.04f * sin(tick)
                            GLES30.glClearColor(0.05f, pulse, 0.16f, 1.0f)
                            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                        }
                    })
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_gl_surface")
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (showHud) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .background(NxHudBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("performance_hud")
                ) {
                    Text(
                        text = String.format(Locale.US, "%d fps • %.1f ms", fpsCap, 1000.0 / fpsCap),
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$gameName ($resolution)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = { panelOpen = !panelOpen },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .minimumInteractiveComponentSize()
                    .background(NxIconBg, RoundedCornerShape(10.dp))
                    .testTag("in_game_panel_button")
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = stringResource(R.string.cd_in_game_panel),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            AnimatedVisibility(
                visible = panelOpen,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NxPanelBg)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.in_game_panel_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { panelOpen = false },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_close_panel)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.detail_title_id, titleId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.in_game_driver_label, driver),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.in_game_hud_toggle),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = showHud,
                                onCheckedChange = { showHud = it },
                                modifier = Modifier.testTag("in_game_hud_switch")
                            )
                        }

                        Text(
                            text = stringResource(R.string.in_game_fps_cap_label, fpsCap),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 60).forEach { cap ->
                                FilterChip(
                                    selected = fpsCap == cap,
                                    onClick = { fpsCap = cap },
                                    label = { Text("$cap FPS") },
                                    modifier = Modifier.testTag("fps_chip_$cap")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onStopGame,
                            colors = ButtonDefaults.buttonColors(containerColor = NxStopButton),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("stop_game_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_stop_game),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
