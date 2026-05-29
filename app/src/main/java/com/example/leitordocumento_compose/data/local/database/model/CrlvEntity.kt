package com.example.leitordocumento_compose.data.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crlv")
data class CrlvEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placa: String? = null,
    val renavam: String? = null,
    val chassi: String? = null,
    val proprietario: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val anoFabricacao: String? = null,
    val anoModelo: String? = null,
    val cor: String? = null,
    val municipio: String? = null,
    val categoria: String? = null,
    val validade: String? = null,
    val rawText: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)