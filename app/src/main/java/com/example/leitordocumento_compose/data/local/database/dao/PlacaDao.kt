package com.example.leitordocumento_compose.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.leitordocumento_compose.data.local.database.model.PlacaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entity: PlacaEntity): Long

    @Update
    suspend fun atualizar(entity: PlacaEntity)

    @Query("SELECT * FROM placa WHERE id = :id")
    suspend fun buscarPorId(id: Long): PlacaEntity?

    @Query("SELECT * FROM placa ORDER BY criadoEm DESC")
    fun listarTodos(): Flow<List<PlacaEntity>>

    @Delete
    suspend fun deletar(entity: PlacaEntity)
}