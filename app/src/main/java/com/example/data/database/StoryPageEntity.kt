package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_pages")
data class StoryPageEntity(
    @PrimaryKey val pageNumber: Int,
    val storyText: String,
    val choice1: String,
    val choice2: String,
    val lightingEffect: String
)
