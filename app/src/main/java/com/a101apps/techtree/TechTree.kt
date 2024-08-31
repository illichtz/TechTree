package com.a101apps.techtree

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Entity(tableName = "tech_trees")
data class TechTree(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @TypeConverters(TechDetailsConverter::class) val details: Map<String, Map<String, String>>
)

class TechDetailsConverter {
    @TypeConverter
    fun fromString(value: String): Map<String, Map<String, String>> {
        val mapType = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMap(map: Map<String, Map<String, String>>): String {
        return Gson().toJson(map)
    }
}

@Dao
interface TechTreeDao {
    @Query("SELECT * FROM tech_trees WHERE id = :id")
    suspend fun getById(id: UUID): TechTree?

    @Query("SELECT * FROM tech_trees")
    fun getAll(): List<TechTree>

    @Insert
    fun insert(techTree: TechTree)

    @Delete
    fun delete(techTree: TechTree)

    @Update
    fun update(techTree: TechTree)
}

@Database(entities = [TechTree::class], version = 1, exportSchema = false)
@TypeConverters(TechDetailsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun techTreeDao(): TechTreeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tech_tree_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
