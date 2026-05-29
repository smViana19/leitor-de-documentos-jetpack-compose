package com.example.leitordocumento_compose.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.leitordocumento_compose.data.local.database.dao.CnhDao
import com.example.leitordocumento_compose.data.local.database.dao.CrlvDao
import com.example.leitordocumento_compose.data.local.database.dao.PlacaDao
import com.example.leitordocumento_compose.data.local.database.dao.RgDao
import com.example.leitordocumento_compose.data.local.database.model.CnhEntity
import com.example.leitordocumento_compose.data.local.database.model.CrlvEntity
import com.example.leitordocumento_compose.data.local.database.model.PlacaEntity
import com.example.leitordocumento_compose.data.local.database.model.RgEntity


@Database(
    entities = [CnhEntity::class, RgEntity::class, PlacaEntity::class, CrlvEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase()
{
    abstract fun cnhDao(): CnhDao
    abstract fun rgDao(): RgDao
    abstract fun placaDao(): PlacaDao
    abstract fun crlvDao(): CrlvDao

    companion object
    {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scanner_db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

object AppContainer
{
    lateinit var db: AppDatabase
        private set

    fun init(context: Context)
    {
        db = AppDatabase.getInstance(context)
    }
}