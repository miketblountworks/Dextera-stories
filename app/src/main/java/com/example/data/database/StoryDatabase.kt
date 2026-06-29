package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters")
    suspend fun clearAllCharacters()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM story_state WHERE id = 1 LIMIT 1")
    fun getStoryState(): Flow<StoryStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryState(state: StoryStateEntity)

    @Query("UPDATE story_state SET currentPageNumber = :pageNumber WHERE id = 1")
    suspend fun updateCurrentPage(pageNumber: Int)

    @Query("DELETE FROM story_state")
    suspend fun clearStoryState()

    @Query("SELECT * FROM story_pages ORDER BY pageNumber ASC")
    fun getAllPages(): Flow<List<StoryPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: StoryPageEntity)

    @Query("DELETE FROM story_pages")
    suspend fun clearPages()
}

@Database(
    entities = [CharacterEntity::class, StoryStateEntity::class, StoryPageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StoryDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun storyDao(): StoryDao

    companion object {
        @Volatile
        private var INSTANCE: StoryDatabase? = null

        fun getDatabase(context: Context): StoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoryDatabase::class.java,
                    "storybook_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
