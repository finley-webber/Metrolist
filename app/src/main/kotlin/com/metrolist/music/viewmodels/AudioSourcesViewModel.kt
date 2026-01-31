package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AudioSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AudioSourcesViewModel @Inject constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val stateSongId = savedStateHandle.get<String>("songId")!!
    // Expose all sources for a song as Flow (use this from Compose)
    fun audioSourcesForSong(songId: String): Flow<List<AudioSources>> {
        return database.audioSourcesBySongId(songId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    // One-shot lookup (suspend)
    suspend fun getById(id: Int): AudioSources? {
        return database.getAudioSourceById(id)
    }

    // Insert a new audio source
    fun addAudioSource(
        songId: String,
        externalAudioPath: String,
        isSelected: Boolean = false,
        type: Int = 0,
        title: String,
    ) {
        val entity = AudioSources(
            songId = songId,
            externalAudioPath = externalAudioPath,
            isSelected = isSelected,
            type = type,
            title = title
        )
        viewModelScope.launch(Dispatchers.IO) {
            database.insertAudioSource(entity)
        }
    }

    // Toggle selected flag


    // Delete source
    fun deleteAudioSource(songId: String, id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            database.deleteAudioSourcesBySongId(songId = songId, id = id)
        }
    }

    fun selectAudioSource(songId: String, id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            database.selectAudioSource(songId, id)
        }
    }

    fun deselectAllSources(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.deselectAllAudioSources(songId)
        }
    }
}
