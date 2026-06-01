package com.example.leitordocumento_compose.data.local.repository

import com.example.leitordocumento_compose.data.OcrResultado
import com.example.leitordocumento_compose.data.local.database.AppContainer
import com.example.leitordocumento_compose.data.local.database.dao.CnhDao
import com.example.leitordocumento_compose.data.local.database.dao.CrlvDao
import com.example.leitordocumento_compose.data.local.database.dao.PlacaDao
import com.example.leitordocumento_compose.data.local.database.dao.RgDao
import com.example.leitordocumento_compose.data.local.database.model.CnhEntity
import com.example.leitordocumento_compose.data.local.database.model.CrlvEntity
import com.example.leitordocumento_compose.data.local.database.model.PlacaEntity
import com.example.leitordocumento_compose.data.local.database.model.RgEntity
import com.example.leitordocumento_compose.data.local.repository.AppRepository.Salvo.*

class AppRepository(
    private val cnhDao: CnhDao,
    private val rgDao: RgDao,
    private val placaDao: PlacaDao,
    private val crlvDao: CrlvDao
)
{
    sealed class Salvo
    {
        data class Cnh(val id: Long) : Salvo()
        data class Rg(val id: Long) : Salvo()
        data class Placa(val id: Long) : Salvo()
        data class Crlv(val id: Long) : Salvo()
    }

    suspend fun salvarResultadoOcr(resultado: OcrResultado): Salvo
    {
        return when (resultado)
        {
            is OcrResultado.Cnh ->
            {
                val d = resultado.dadosCNH
                val id = cnhDao.salvarCnh(
                    CnhEntity(
                        nome = d.nome, cpf = d.cpf, rg = d.rg,
                        orgaoEmissor = d.orgaoEmissor,
                        numeroRegistro = d.numeroRegistro,
                        categoria = d.categoria,
                        primeiraHabilitacao = d.primeiraHabilitacao,
                        dataEmissao = d.dataEmissao,
                        dataValidade = d.dataValidade,
                        dataNascimento = d.dataNascimento,
                        localNascimento = d.localNascimento,
                        filiacao = d.filiacao,
                        rawText = d.rawText
                    )
                )
                Cnh(id)
            }

            is OcrResultado.Placa ->
            {
                val id = placaDao.inserir(
                    PlacaEntity(
                        placa = resultado.dadosPlaca.placa,
                        placaNormalizada = resultado.dadosPlaca.placaNormalizada
                    )
                )
                Placa(id)
            }

            is OcrResultado.Rg ->
            {
                val id = rgDao.salvarRg(
                    RgEntity(
                        nome = resultado.dadosRG.nome,
                        cpf = resultado.dadosRG.cpf,
                        rg = resultado.dadosRG.rg,
                        dataEmissao = resultado.dadosRG.dataEmissao,
                        dataNascimento = resultado.dadosRG.dataNascimento,
                        nomeMae = resultado.dadosRG.nomeMae,
                        nomePai = resultado.dadosRG.nomePai,
                        naturalidade = resultado.dadosRG.naturalidade,
                        rawText = resultado.dadosRG.rawText
                    )
                )
                Rg(id)
            }

            is OcrResultado.Desconhecido ->
            {
                val id = cnhDao.salvarCnh(CnhEntity(rawText = resultado.rawText))
                Cnh(id)
            }
            is OcrResultado.Crlv ->
            {
                val id = crlvDao.inserir(CrlvEntity(placa = resultado.dadosCRLV.placa,
                    renavam = resultado.dadosCRLV.renavam,
                    chassi = resultado.dadosCRLV.chassi,
                    proprietario = resultado.dadosCRLV.proprietario,
                    marca = resultado.dadosCRLV.marca,
                    modelo = resultado.dadosCRLV.modelo,
                    anoFabricacao = resultado.dadosCRLV.anoFabricacao,
                    anoModelo = resultado.dadosCRLV.anoModelo,
                    cor = resultado.dadosCRLV.corPredominante,
                    municipio = resultado.dadosCRLV.municipio,
                    validade = resultado.dadosCRLV.validade,
                    rawText = resultado.dadosCRLV.rawText
                    ))
                Crlv(id)
            }
        }
    }

    suspend fun buscarCnh(id: Long) = cnhDao.buscarCnhPorId(id)
    suspend fun buscarRg(id: Long) = rgDao.buscarRgPorId(id)
    suspend fun buscarPlaca(id: Long) = placaDao.buscarPorId(id)
    suspend fun buscarCrlv(id: Long) = crlvDao.buscarPorId(id)

    suspend fun atualizarCnh(entity: CnhEntity) = cnhDao.editarCnh(entity)
    suspend fun atualizarRg(entity: RgEntity) = rgDao.editarRg(entity)
    suspend fun atualizarPlaca(entity: PlacaEntity) = placaDao.atualizar(entity)
    suspend fun atualizarCrlv(entity: CrlvEntity) = crlvDao.atualizar(entity)

    fun listarCnhs() = cnhDao.buscarTodos()
    fun listarRgs() = rgDao.listarTodos()
    fun listarPlacas() = placaDao.listarTodos()
    fun listarCrlvs() = crlvDao.listarTodos()

    companion object
    {
        fun fromAppContainer() = AppRepository(
            AppContainer.db.cnhDao(),
            AppContainer.db.rgDao(),
            AppContainer.db.placaDao(),
            AppContainer.db.crlvDao()
        )
    }

}