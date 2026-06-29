package com.example.data.repository

import com.example.data.database.CharacterDao
import com.example.data.database.CharacterEntity
import com.example.data.database.StoryDao
import com.example.data.database.StoryPageEntity
import com.example.data.database.StoryStateEntity
import kotlinx.coroutines.flow.Flow

class StoryRepository(
    private val characterDao: CharacterDao,
    private val storyDao: StoryDao
) {
    val allCharacters: Flow<List<CharacterEntity>> = characterDao.getAllCharacters()
    val storyState: Flow<StoryStateEntity?> = storyDao.getStoryState()
    val allPages: Flow<List<StoryPageEntity>> = storyDao.getAllPages()

    suspend fun insertCharacter(character: CharacterEntity) {
        characterDao.insertCharacter(character)
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        characterDao.deleteCharacter(character)
    }

    suspend fun insertStoryState(state: StoryStateEntity) {
        storyDao.insertStoryState(state)
    }

    suspend fun updateCurrentPage(pageNumber: Int) {
        storyDao.updateCurrentPage(pageNumber)
    }

    suspend fun insertPage(page: StoryPageEntity) {
        storyDao.insertPage(page)
    }

    suspend fun resetStory() {
        storyDao.clearPages()
        storyDao.clearStoryState()
    }

    suspend fun clearAll() {
        storyDao.clearPages()
        storyDao.clearStoryState()
        characterDao.clearAllCharacters()
    }
}
