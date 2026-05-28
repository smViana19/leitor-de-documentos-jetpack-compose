package com.example.leitordocumento_compose.data.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rg")
data class RgEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String? = null,
    val rg: String? = null,
    val cpf: String? = null,
    val dataNascimento: String? = null,
    val naturalidade: String? = null,
    val nomeMae: String? = null,
    val nomePai: String? = null,
    val dataEmissao: String? = null,
    val rawText: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)