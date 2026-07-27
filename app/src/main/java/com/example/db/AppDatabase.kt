package com.example.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shortcuts")
data class Shortcut(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val phrase: String,
    val isPinned: Boolean = false
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY isPinned DESC, id DESC")
    fun getAll(): Flow<List<Shortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: Shortcut)

    @Update
    suspend fun update(shortcut: Shortcut)

    @Delete
    suspend fun delete(shortcut: Shortcut)
}

@Database(entities = [Shortcut::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_db")
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
