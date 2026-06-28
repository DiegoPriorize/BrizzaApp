package com.priorizedev.brizza.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.PmocReport
import com.priorizedev.brizza.data.model.PmocLogbookEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Clientes
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun getClienteById(id: String): Flow<Cliente?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClienteIgnore(cliente: Cliente): Long

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Transaction
    suspend fun insertCliente(cliente: Cliente) {
        val id = insertClienteIgnore(cliente)
        if (id == -1L) {
            updateCliente(cliente)
        }
    }

    @Delete
    suspend fun deleteCliente(cliente: Cliente)

    // Ambientes
    @Query("SELECT * FROM ambientes")
    fun getAllAmbientes(): Flow<List<Ambiente>>

    @Query("SELECT * FROM ambientes WHERE clienteId = :clienteId ORDER BY nome ASC")
    fun getAmbientesByCliente(clienteId: String): Flow<List<Ambiente>>

    @Query("SELECT * FROM ambientes WHERE id = :id")
    suspend fun getAmbienteById(id: String): Ambiente?

    @Query("SELECT * FROM ambientes WHERE id = :id")
    fun getAmbienteByIdFlow(id: String): Flow<Ambiente?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmbiente(ambiente: Ambiente)

    @Delete
    suspend fun deleteAmbiente(ambiente: Ambiente)

    // Equipamentos
    @Query("SELECT * FROM equipamentos ORDER BY tag ASC")
    fun getAllEquipamentos(): Flow<List<Equipamento>>

    @Query("SELECT * FROM equipamentos WHERE clienteId = :clienteId ORDER BY tag ASC")
    fun getEquipamentosByCliente(clienteId: String): Flow<List<Equipamento>>

    @Query("SELECT * FROM equipamentos WHERE ambienteId = :ambienteId ORDER BY tag ASC")
    fun getEquipamentosByAmbiente(ambienteId: String): Flow<List<Equipamento>>

    @Query("SELECT * FROM equipamentos WHERE id = :id")
    suspend fun getEquipamentoById(id: String): Equipamento?

    @Query("SELECT * FROM equipamentos WHERE id = :id")
    fun getEquipamentoByIdFlow(id: String): Flow<Equipamento?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipamento(equipamento: Equipamento)

    @Delete
    suspend fun deleteEquipamento(equipamento: Equipamento)

    // Tecnicos
    @Query("SELECT * FROM tecnicos ORDER BY nome ASC")
    fun getAllTecnicos(): Flow<List<Tecnico>>

    @Query("SELECT * FROM tecnicos WHERE id = :id")
    suspend fun getTecnicoById(id: String): Tecnico?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTecnico(tecnico: Tecnico)

    @Delete
    suspend fun deleteTecnico(tecnico: Tecnico)

    // Ordens de Serviço
    @Query("SELECT * FROM ordens_servico ORDER BY dataCriacao DESC")
    fun getAllOrdensServico(): Flow<List<OrdemServico>>

    @Query("SELECT * FROM ordens_servico WHERE clienteId = :clienteId ORDER BY dataCriacao DESC")
    fun getOrdensServicoByCliente(clienteId: String): Flow<List<OrdemServico>>

    @Query("SELECT * FROM ordens_servico WHERE id = :id")
    fun getOrdemServicoById(id: String): Flow<OrdemServico?>

    @Query("SELECT * FROM ordens_servico WHERE equipamentoId = :equipamentoId ORDER BY dataCriacao DESC")
    fun getOrdensServicoByEquipamento(equipamentoId: String): Flow<List<OrdemServico>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrdemServico(ordemServico: OrdemServico)

    @Delete
    suspend fun deleteOrdemServico(ordemServico: OrdemServico)

    // PMOC Reports
    @Query("SELECT * FROM pmoc_reports ORDER BY dataGeracao DESC")
    fun getAllPmocReports(): Flow<List<PmocReport>>

    @Query("SELECT * FROM pmoc_reports WHERE clienteId = :clienteId ORDER BY dataGeracao DESC")
    fun getPmocReportsByCliente(clienteId: String): Flow<List<PmocReport>>

    @Query("SELECT * FROM pmoc_reports WHERE id = :id")
    fun getPmocReportById(id: String): Flow<PmocReport?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPmocReport(report: PmocReport)

    @Delete
    suspend fun deletePmocReport(report: PmocReport)

    // PMOC Logbook Entries
    @Query("SELECT * FROM pmoc_logbook_entries")
    fun getAllLogbookEntries(): Flow<List<PmocLogbookEntry>>

    @Query("SELECT * FROM pmoc_logbook_entries WHERE pmocId = :pmocId")
    fun getLogbookEntriesByPmoc(pmocId: String): Flow<List<PmocLogbookEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogbookEntry(entry: PmocLogbookEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogbookEntries(entries: List<PmocLogbookEntry>)

    @Query("DELETE FROM pmoc_logbook_entries WHERE pmocId = :pmocId AND equipamentoId = :equipamentoId AND atividade = :atividade")
    suspend fun deleteLogbookEntry(pmocId: String, equipamentoId: String, atividade: String)

    // Usuarios
    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun getUsuarioByEmail(email: String): com.priorizedev.brizza.data.model.Usuario?

    @Query("SELECT * FROM usuarios")
    fun getAllUsuarios(): Flow<List<com.priorizedev.brizza.data.model.Usuario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: com.priorizedev.brizza.data.model.Usuario)

    // Peças
    @Query("SELECT * FROM pecas ORDER BY nome ASC")
    fun getAllPecas(): Flow<List<com.priorizedev.brizza.data.model.Peca>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeca(peca: com.priorizedev.brizza.data.model.Peca)

    @Delete
    suspend fun deletePeca(peca: com.priorizedev.brizza.data.model.Peca)

    // Programas Preventivos
    @Query("SELECT * FROM programas_preventivos")
    fun getAllProgramasPreventivos(): Flow<List<com.priorizedev.brizza.data.model.ProgramaPreventivo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo)

    @Delete
    suspend fun deleteProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo)

    // Marcas de Equipamentos
    @Query("SELECT * FROM marcas_equipamentos WHERE usuarioEmail = :email OR usuarioEmail = '' OR usuarioEmail IS NULL ORDER BY nome ASC")
    fun getAllMarcasEquipamentos(email: String): Flow<List<com.priorizedev.brizza.data.model.MarcaEquipamento>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarcaEquipamento(marca: com.priorizedev.brizza.data.model.MarcaEquipamento)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarcasEquipamentos(marcas: List<com.priorizedev.brizza.data.model.MarcaEquipamento>)

    @Delete
    suspend fun deleteMarcaEquipamento(marca: com.priorizedev.brizza.data.model.MarcaEquipamento)

    @Query("SELECT * FROM marcas_equipamentos")
    fun getAllMarcasEquipamentosRaw(): Flow<List<com.priorizedev.brizza.data.model.MarcaEquipamento>>

    // Log de Versões
    @Query("SELECT * FROM log_versao ORDER BY id DESC")
    fun getAllLogsVersao(): Flow<List<com.priorizedev.brizza.data.model.LogVersao>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogVersao(log: com.priorizedev.brizza.data.model.LogVersao)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogsVersao(logs: List<com.priorizedev.brizza.data.model.LogVersao>)
}

@Database(
    entities = [
        Cliente::class,
        Ambiente::class,
        Equipamento::class,
        Tecnico::class,
        OrdemServico::class,
        PmocReport::class,
        PmocLogbookEntry::class,
        com.priorizedev.brizza.data.model.Usuario::class,
        com.priorizedev.brizza.data.model.Peca::class,
        com.priorizedev.brizza.data.model.ProgramaPreventivo::class,
        com.priorizedev.brizza.data.model.MarcaEquipamento::class,
        com.priorizedev.brizza.data.model.LogVersao::class
    ],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
