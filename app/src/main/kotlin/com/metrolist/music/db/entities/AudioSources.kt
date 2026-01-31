package com.metrolist.music.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audioSources")
data class AudioSources(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "songId") val songId: String,
    @ColumnInfo(name = "externalAudioPath") val externalAudioPath: String,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "title") val title: String,
)
