/*
 * Copyright (c) 2026 Auxio Project
 * SettingsScreen.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.oxycblt.auxio.ui.ios

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import org.oxycblt.auxio.R
import org.oxycblt.auxio.IntegerTable
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.playback.replaygain.ReplayGainMode

private const val PREF_KEY_REPLAY_GAIN = "auxio_replay_gain"

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }

    val isPowerampInstalled = remember(pm) {
        pm.getLaunchIntentForPackage("com.maxmpz.equalizer") != null
    }

    val isPlaying by playbackViewModel.isPlaying.collectAsStateWithLifecycle()
    val isEqActive = isPowerampInstalled && isPlaying

    val initialReplayGain = remember(prefs) {
        try {
            val code = prefs.getInt(PREF_KEY_REPLAY_GAIN, IntegerTable.REPLAY_GAIN_MODE_OFF)
            code != IntegerTable.REPLAY_GAIN_MODE_OFF
        } catch (_: Exception) {
            false
        }
    }
    var replayGain by remember { mutableStateOf(initialReplayGain) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }

        item {
            SettingsGroup(title = "AUDIO & EQUALIZER") {
                SettingsSwitchRow(
                    title = "ReplayGain Normalization",
                    subtitle = "Normalize volume (disabled for bit-perfect output)",
                    checked = replayGain,
                    onCheckedChange = { checked ->
                        replayGain = checked
                        val code = if (checked) IntegerTable.REPLAY_GAIN_MODE_DYNAMIC else IntegerTable.REPLAY_GAIN_MODE_OFF
                        prefs.edit()
                            .putInt(PREF_KEY_REPLAY_GAIN, code)
                            .apply()
                    }
                )
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = "Equalizer",
                    subtitle = if (isEqActive) {
                        "Poweramp Equalizer (Active)"
                    } else if (isPowerampInstalled) {
                        "Poweramp Equalizer"
                    } else {
                        "System Equalizer"
                    },
                    indicator = {
                        if (isEqActive) {
                            val infiniteTransition = rememberInfiniteTransition(label = "eq_pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(900, easing = FastOutSlowInEasing),
                                    repeatMode = AnimRepeatMode.Reverse
                                ),
                                label = "pulse_alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759).copy(alpha = pulseAlpha))
                            )
                        } else if (isPowerampInstalled) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8E8E93).copy(alpha = 0.5f))
                            )
                        }
                    },
                    onClick = {
                        val powerampIntent = pm.getLaunchIntentForPackage("com.maxmpz.equalizer")
                        if (powerampIntent != null) {
                            powerampIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(powerampIntent)
                            } catch (_: Exception) {}
                        } else {
                            val systemEqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(systemEqIntent)
                            } catch (_: Exception) {}
                        }
                    }
                )
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = "Android Developer Options",
                    subtitle = "Configure LDAC 990kbps & Disable Absolute Volume",
                    stackedSubtitle = true,
                    onClick = {
                        try {
                            val devIntent = Intent(AndroidSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(devIntent)
                        } catch (e: Exception) {
                            try {
                                val sysSettings = Intent(AndroidSettings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(sysSettings)
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroup(title = "LIBRARY") {
                val stats by musicViewModel.statistics.collectAsState()
                SettingsRow(
                    title = "Statistics",
                    subtitle = "${stats?.songs ?: 0} songs, ${stats?.albums ?: 0} albums"
                ) {}
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = "Rescan Library",
                    subtitle = ""
                ) {
                    musicViewModel.rescan()
                }
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = "Refresh",
                    subtitle = ""
                ) {
                    musicViewModel.refresh()
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsGroup(title = "APPEARANCE") {
                SettingsRow(
                    title = "Theme",
                    subtitle = "Apple Dark"
                ) {}
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
                SettingsRow(
                    title = "Material",
                    subtitle = "Liquid Glass"
                ) {}
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
    ) {
        content()
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    indicator: (@Composable () -> Unit)? = null,
    stackedSubtitle: Boolean = false,
    onClick: () -> Unit
) {
    if (stackedSubtitle) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                    if (indicator != null) {
                        indicator()
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 17.sp
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (indicator != null) {
                    indicator()
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 12.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 17.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF39393D),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}
