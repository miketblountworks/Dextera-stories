package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gender: String, // Male, Female, Other
    val alignment: String, // Good, Evil
    val personalityTraits: String // Comma-separated traits, e.g. "brave, loyal, curious"
)
