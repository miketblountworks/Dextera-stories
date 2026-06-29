package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_state")
data class StoryStateEntity(
    @PrimaryKey val id: Int = 1, // Only single active story session for simplicity
    val protagonistName: String,
    val protagonistAge: Int,
    val currentPageNumber: Int = 1,
    val isCompleted: Boolean = false
)
