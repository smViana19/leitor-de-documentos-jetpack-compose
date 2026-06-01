package com.example.leitordocumento_compose.data.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cnh")
data class CnhEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String? = null,
    val cpf: String? = null,
    val rg: String? = null,
    val orgaoEmissor: String? = null,
    val numeroRegistro: String? = null,
    val categoria: String? = null,
    val primeiraHabilitacao: String? = null,
    val dataEmissao: String? = null,
    val dataValidade: String? = null,
    val dataNascimento: String? = null,
    val localNascimento: String? = null,
    val filiacao: String? = null,
    val rawText: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)