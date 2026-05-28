package com.example.leitordocumento_compose.data.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placa")
data class PlacaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placa: String = "",
    val placaNormalizada: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)