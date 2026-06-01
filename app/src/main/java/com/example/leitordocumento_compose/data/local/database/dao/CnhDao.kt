package com.example.leitordocumento_compose.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.leitordocumento_compose.data.local.database.model.CnhEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CnhDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarCnh(cnhEntity: CnhEntity): Long

    @Update
    suspend fun editarCnh(cnhEntity: CnhEntity)

    @Query("SELECT * FROM cnh WHERE id = :id")
    suspend fun buscarCnhPorId(id: Long): CnhEntity?

    @Query("SELECT * FROM cnh")
    fun buscarTodos(): Flow<List<CnhEntity>>

    @Delete
    suspend fun deletarCnh(entity: CnhEntity)

}