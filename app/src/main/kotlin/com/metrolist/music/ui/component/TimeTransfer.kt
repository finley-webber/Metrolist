package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.LocalDatabase
import com.metrolist.music.db.entities.SongWithStats
import com.metrolist.music.viewmodels.StatsViewModel
import java.util.concurrent.TimeUnit

@Composable
fun TimeTransfer(
    onDismiss: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val sourceSong = remember { mutableStateOf<SongWithStats?>(null) }
    val targetSong = remember { mutableStateOf<SongWithStats?>(null) }

    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()

    DefaultDialog(
        onDismiss = onDismiss,
        title = {Text("Time Transfer")},
        content = {
            Column {
                SongSelectDropdown(
                    titleT = "Source Song",
                    songs = mostPlayedSongsStats,
                    onSelectionChange = {},
                    selectedSong = sourceSong
                )

                Row {
                    Text("Listen Time: ")
                    if (sourceSong.value != null) {
                        Text(formatMillis(sourceSong.value!!.timeListened))
                    }
                }

                SongSelectDropdown(
                    titleT = "Target Song",
                    songs = mostPlayedSongsStats,
                    onSelectionChange = {},
                    selectedSong = targetSong,
                )

                Row {
                    Text("Listen Time: ")
                    if (targetSong.value != null) {
                        Text(formatMillis(targetSong.value!!.timeListened))
                    }
                }

                IconButton(
                    onClick = {
                        val from = sourceSong.value?.id
                        val to = targetSong.value?.id
                        if (from != null && to != null && from != to) {
                            viewModel.transferSongStats(from, to) {
                                // optional: clear selection / close dialog
                                sourceSong.value = null
                                targetSong.value = null
                            }
                        }
                    },
                    onLongClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sourceSong.value != null &&
                            targetSong.value != null &&
                            sourceSong.value!!.id != targetSong.value!!.id,
                    content = { Text("Convert") }
                )
            }
        }

    )
}

fun formatMillis(ms: Long?): String {
    if (ms == null) {
        return "00:00:00"
    }
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}