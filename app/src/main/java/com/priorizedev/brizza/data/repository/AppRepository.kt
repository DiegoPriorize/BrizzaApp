package com.priorizedev.brizza.data.repository

import com.priorizedev.brizza.data.db.AppDatabase
import com.priorizedev.brizza.data.db.AppDao
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.PmocReport
import com.priorizedev.brizza.data.model.PmocLogbookEntry
import kotlinx.coroutines.flow.Flow

class AppRepository(val database: AppDatabase) {
    private val appDao: AppDao = database.appDao()

    // Clientes
    val allClientes: Flow<List<Cliente>> = appDao.getAllClientes()
    
    fun getClienteById(id: String): Flow<Cliente?> = appDao.getClienteById(id)
    
    suspend fun insertCliente(cliente: Cliente) = appDao.insertCliente(cliente)
    
    suspend fun updateCliente(cliente: Cliente) = appDao.updateCliente(cliente)
    
    suspend fun deleteCliente(cliente: Cliente) = appDao.deleteCliente(cliente)

    // Ambientes
    val allAmbientes: Flow<List<Ambiente>> = appDao.getAllAmbientes()
    
    fun getAmbientesByCliente(clienteId: String): Flow<List<Ambiente>> = appDao.getAmbientesByCliente(clienteId)
    
    suspend fun getAmbienteById(id: String) = appDao.getAmbienteById(id)

    fun getAmbienteByIdFlow(id: String): Flow<Ambiente?> = appDao.getAmbienteByIdFlow(id)
    
    suspend fun insertAmbiente(ambiente: Ambiente) = appDao.insertAmbiente(ambiente)
    
    suspend fun deleteAmbiente(ambiente: Ambiente) = appDao.deleteAmbiente(ambiente)

    // Equipamentos
    val allEquipamentos: Flow<List<Equipamento>> = appDao.getAllEquipamentos()
    
    fun getEquipamentosByCliente(clienteId: String): Flow<List<Equipamento>> = appDao.getEquipamentosByCliente(clienteId)
    
    fun getEquipamentosByAmbiente(ambienteId: String): Flow<List<Equipamento>> = appDao.getEquipamentosByAmbiente(ambienteId)
    
    suspend fun getEquipamentoById(id: String) = appDao.getEquipamentoById(id)
    
    fun getEquipamentoByIdFlow(id: String): Flow<Equipamento?> = appDao.getEquipamentoByIdFlow(id)
    
    suspend fun insertEquipamento(equipamento: Equipamento) = appDao.insertEquipamento(equipamento)
    
    suspend fun deleteEquipamento(equipamento: Equipamento) = appDao.deleteEquipamento(equipamento)

    // Tecnicos
    val allTecnicos: Flow<List<Tecnico>> = appDao.getAllTecnicos()
    
    suspend fun getTecnicoById(id: String) = appDao.getTecnicoById(id)
    
    suspend fun insertTecnico(tecnico: Tecnico) = appDao.insertTecnico(tecnico)
    
    suspend fun deleteTecnico(tecnico: Tecnico) = appDao.deleteTecnico(tecnico)

    // Ordens de Serviço
    val allOrdensServico: Flow<List<OrdemServico>> = appDao.getAllOrdensServico()
    
    fun getOrdensServicoByCliente(clienteId: String): Flow<List<OrdemServico>> = appDao.getOrdensServicoByCliente(clienteId)
    
    fun getOrdemServicoById(id: String): Flow<OrdemServico?> = appDao.getOrdemServicoById(id)
    
    fun getOrdensServicoByEquipamento(equipamentoId: String): Flow<List<OrdemServico>> = appDao.getOrdensServicoByEquipamento(equipamentoId)
    
    suspend fun insertOrdemServico(ordemServico: OrdemServico) = appDao.insertOrdemServico(ordemServico)
    
    suspend fun deleteOrdemServico(ordemServico: OrdemServico) = appDao.deleteOrdemServico(ordemServico)

    // PMOC Reports
    val allPmocReports: Flow<List<PmocReport>> = appDao.getAllPmocReports()
    
    fun getPmocReportsByCliente(clienteId: String): Flow<List<PmocReport>> = appDao.getPmocReportsByCliente(clienteId)
    
    fun getPmocReportById(id: String): Flow<PmocReport?> = appDao.getPmocReportById(id)
    
    suspend fun insertPmocReport(report: PmocReport) = appDao.insertPmocReport(report)
    
    suspend fun deletePmocReport(report: PmocReport) = appDao.deletePmocReport(report)

    // PMOC Logbook Entries
    fun getLogbookEntriesByPmoc(pmocId: String): Flow<List<PmocLogbookEntry>> = appDao.getLogbookEntriesByPmoc(pmocId)
    
    suspend fun insertLogbookEntry(entry: PmocLogbookEntry) = appDao.insertLogbookEntry(entry)
    
    suspend fun insertLogbookEntries(entries: List<PmocLogbookEntry>) = appDao.insertLogbookEntries(entries)
    
    suspend fun deleteLogbookEntry(pmocId: String, equipamentoId: String, atividade: String) = 
        appDao.deleteLogbookEntry(pmocId, equipamentoId, atividade)

    // Usuarios
    val allUsuarios: Flow<List<com.priorizedev.brizza.data.model.Usuario>> = appDao.getAllUsuarios()

    suspend fun getUsuarioByEmail(email: String): com.priorizedev.brizza.data.model.Usuario? = appDao.getUsuarioByEmail(email)
    
    suspend fun insertUsuario(usuario: com.priorizedev.brizza.data.model.Usuario) = appDao.insertUsuario(usuario)

    // Peças
    val allPecas: Flow<List<com.priorizedev.brizza.data.model.Peca>> = appDao.getAllPecas()

    suspend fun insertPeca(peca: com.priorizedev.brizza.data.model.Peca) = appDao.insertPeca(peca)

    suspend fun deletePeca(peca: com.priorizedev.brizza.data.model.Peca) = appDao.deletePeca(peca)

    // Programas Preventivos
    val allProgramasPreventivos: Flow<List<com.priorizedev.brizza.data.model.ProgramaPreventivo>> = appDao.getAllProgramasPreventivos()

    suspend fun insertProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo) = 
        appDao.insertProgramaPreventivo(programa)

    suspend fun deleteProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo) = 
        appDao.deleteProgramaPreventivo(programa)

    // Marcas de Equipamentos
    fun getAllMarcasEquipamentos(email: String): Flow<List<com.priorizedev.brizza.data.model.MarcaEquipamento>> = 
        appDao.getAllMarcasEquipamentos(email)

    suspend fun insertMarcaEquipamento(marca: com.priorizedev.brizza.data.model.MarcaEquipamento) = 
        appDao.insertMarcaEquipamento(marca)

    suspend fun insertMarcasEquipamentos(marcas: List<com.priorizedev.brizza.data.model.MarcaEquipamento>) = 
        appDao.insertMarcasEquipamentos(marcas)

    suspend fun deleteMarcaEquipamento(marca: com.priorizedev.brizza.data.model.MarcaEquipamento) = 
        appDao.deleteMarcaEquipamento(marca)

    val allMarcasEquipamentosRaw: Flow<List<com.priorizedev.brizza.data.model.MarcaEquipamento>> = 
        appDao.getAllMarcasEquipamentosRaw()

    // Logs de Versões
    val allLogsVersao: Flow<List<com.priorizedev.brizza.data.model.LogVersao>> = appDao.getAllLogsVersao()

    suspend fun insertLogsVersao(logs: List<com.priorizedev.brizza.data.model.LogVersao>) = 
        appDao.insertLogsVersao(logs)

    suspend fun insertLogVersao(log: com.priorizedev.brizza.data.model.LogVersao) = 
        appDao.insertLogVersao(log)
}
