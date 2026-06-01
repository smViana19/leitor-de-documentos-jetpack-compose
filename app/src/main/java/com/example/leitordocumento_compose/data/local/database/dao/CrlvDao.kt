package com.example.leitordocumento_compose.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.leitordocumento_compose.data.local.database.model.CrlvEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrlvDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entity: CrlvEntity): Long

    @Update
    suspend fun atualizar(entity: CrlvEntity)

    @Query("SELECT * FROM crlv WHERE id = :id")
    suspend fun buscarPorId(id: Long): CrlvEntity?

    @Query("SELECT * FROM crlv ORDER BY criadoEm DESC")
    fun listarTodos(): Flow<List<CrlvEntity>>

    @Delete
    suspend fun deletar(entity: CrlvEntity)
}