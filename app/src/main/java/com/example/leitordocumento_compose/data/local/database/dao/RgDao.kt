package com.example.leitordocumento_compose.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.leitordocumento_compose.data.local.database.model.RgEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarRg(entity: RgEntity): Long

    @Update
    suspend fun editarRg(entity: RgEntity)

    @Query("SELECT * FROM rg WHERE id = :id")
    suspend fun buscarRgPorId(id: Long): RgEntity?

    @Query("SELECT * FROM rg ORDER BY criadoEm DESC")
    fun listarTodos(): Flow<List<RgEntity>>

    @Delete
    suspend fun deletarRg(entity: RgEntity)
}