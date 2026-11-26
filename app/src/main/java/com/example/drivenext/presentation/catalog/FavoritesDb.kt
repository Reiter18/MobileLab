package com.example.drivenext.presentation.catalog

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "favorites", primaryKeys = ["owner", "carId"])
internal data class Favorite(val owner: String, val carId: Long)

@Dao
internal interface FavoritesDao {
    @Query("SELECT carId FROM favorites WHERE owner = :owner")
    fun ids(owner: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun add(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE owner = :owner AND carId = :car")
    fun remove(owner: String, car: Long)
}

@Database(entities = [Favorite::class], version = 1, exportSchema = false)
internal abstract class FavoritesDb : RoomDatabase() {
    abstract fun favorites(): FavoritesDao

    companion object {
        @Volatile private var instance: FavoritesDb? = null

        fun get(context: Context): FavoritesDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FavoritesDb::class.java, "favorites.db")
                .build().also { instance = it }
        }
    }
}
