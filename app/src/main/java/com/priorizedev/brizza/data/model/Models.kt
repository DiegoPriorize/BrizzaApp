package com.priorizedev.brizza.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import java.io.Serializable

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "nome") val nome: String,
    @Json(name = "documento") val documento: String, // CPF/CNPJ
    @Json(name = "telefone") val telefone: String,
    @Json(name = "email") val email: String,
    @Json(name = "endereco") val endereco: String,
    @Json(name = "nome_completo") val nomeCompleto: String = "",
    @Json(name = "tipo_pessoa") val tipoPessoa: String = "Física",
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(
    tableName = "ambientes",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Ambiente(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "cliente_id") val clienteId: String,
    @Json(name = "nome") val nome: String, // e.g. "Sala 101", "Servidor"
    @Json(name = "area_m2") val areaM2: Double,
    @Json(name = "carga_termica_btu") val cargaTermicaBtu: Int,
    @Json(name = "fixos") val fixos: Int = 0,
    @Json(name = "flutuantes") val flutuantes: Int = 0,
    @Json(name = "janelas") val janelas: Int = 0,
    @Json(name = "portas") val portas: Int = 0,
    @Json(name = "fontes_calor") val fontesCalor: Int = 0,
    @Json(name = "incidencia_solar") val incidenciaSolar: String = "Baixa", // "Baixa", "Média", "Alta"
    @Json(name = "endereco") val endereco: String = "",
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(
    tableName = "equipamentos",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Equipamento(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "cliente_id") val clienteId: String,
    @Json(name = "ambiente_id") val ambienteId: String, // Refers to Ambiente but is simplified (not forced cascade for easier deletion of ambients)
    @Json(name = "tag") val tag: String, // e.g. "AC-01"
    @Json(name = "marca") val marca: String,
    @Json(name = "modelo") val modelo: String,
    @Json(name = "numero_serie") val numeroSerie: String,
    @Json(name = "tipo") val tipo: String, // "Split", "Janela", "Cassete", "Piso Teto", "Chiller"
    @Json(name = "capacidade_btu") val capacidadeBtu: Int,
    @Json(name = "fluido_refrigerante") val fluidoRefrigerante: String, // "R-410A", "R-22", "R-32", etc.
    @Json(name = "status") val status: String = "Ativo", // "Ativo", "Inativo", "Manutenção"
    @Json(name = "data_instalacao") val dataInstalacao: String = "",
    @Json(name = "observacoes") val observacoes: String = "",
    @Json(name = "potencia_w") val potenciaW: String = "",
    @Json(name = "tensao") val tensao: String = "220",
    @Json(name = "sistema") val sistema: String = "Inverter",
    @Json(name = "ciclo") val ciclo: String = "Frio",
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(tableName = "tecnicos")
data class Tecnico(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "nome") val nome: String, // Nome para exibição (obg)
    @Json(name = "nome_completo") val nomeCompleto: String = "",
    @Json(name = "cpf") val cpf: String = "",
    @Json(name = "data_nascimento") val dataNascimento: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "telefone") val telefone: String = "", // Contato
    @Json(name = "crea_cft") val creaCft: String = "", // Registro CFT/CREA
    @Json(name = "tem_cnh") val temCnh: Boolean = false, // CNH (toggle)
    @Json(name = "disponivel_viagens") val disponivelViagens: Boolean = false, // Disponível para viagens (toggle)
    @Json(name = "obs") val obs: String = "", // Obs
    @Json(name = "status") val status: String = "Ativo", // Status: Ativo / Inativo
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

fun generateRandomId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..15).map { chars.random() }.joinToString("")
}

fun generateOrdemServicoId(): String {
    return generateRandomId()
}

fun generateRandomNumeroOrdem(): String {
    val part1 = (1000..9999).random()
    val part2 = (0..9).random()
    return "$part1-$part2"
}

@Entity(
    tableName = "ordens_servico",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrdemServico(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "numero_ordem") val numeroOrdem: String = generateRandomNumeroOrdem(),
    @Json(name = "cliente_id") val clienteId: String,
    @Json(name = "tecnico_id") val tecnicoId: String,
    @Json(name = "equipamento_id") val equipamentoId: String,
    @Json(name = "tipo_servico") val tipoServico: String, // "Preventiva", "Corretiva", "Instalação", "Orçamento"
    @Json(name = "data_criacao") val dataCriacao: Long = System.currentTimeMillis(),
    @Json(name = "data_agendada") val dataAgendada: String,
    @Json(name = "descricao") val descricao: String,
    @Json(name = "status") val status: String, // "Aberta", "Em Andamento", "Concluída", "Cancelada"
    @Json(name = "diagnostico_tecnico") val diagnosticoTecnico: String = "",
    @Json(name = "pecas_trocadas") val pecasTrocadas: String = "",
    @Json(name = "valor_servico") val valorServico: Double = 0.0,
    @Json(name = "assinatura_digital") val assinaturaDigital: String = "", // Base64 signature
    @Json(name = "assinatura_cliente_nome") val assinaturaClienteNome: String = "",
    @Json(name = "data_chamado") val dataChamado: String = "",
    @Json(name = "prioridade") val prioridade: String = "Média",
    @Json(name = "descricao_resumida") val descricaoResumida: String = "",
    @Json(name = "corrente_eletrica") val correnteEletrica: String = "",
    @Json(name = "temperatura") val temperatura: String = "",
    @Json(name = "pressao_gas_alta") val pressaoGasAlta: String = "",
    @Json(name = "pressao_gas_baixa") val pressaoGasBaixa: String = "",
    @Json(name = "foto1") val foto1: String = "",
    @Json(name = "foto2") val foto2: String = "",
    @Json(name = "foto3") val foto3: String = "",
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(
    tableName = "pmoc_reports",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PmocReport(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "cliente_id") val clienteId: String,
    @Json(name = "tecnico_id") val tecnicoId: String,
    @Json(name = "data_geracao") val dataGeracao: Long = System.currentTimeMillis(),
    @Json(name = "mes_ref") val mesRef: String, // e.g. "Julho/2026"
    @Json(name = "numero_pmoc") val numeroPmoc: String, // e.g. "PMOC-2026-0001"
    
    // Checkbox values representing PMOC requirements (C = Conforme, NC = Não Conforme, NA = Não Aplicável)
    @Json(name = "filtro_status") val filtroStatus: String = "C",
    @Json(name = "filtro_obs") val filtroObs: String = "",
    
    @Json(name = "serpentina_status") val serpentinaStatus: String = "C",
    @Json(name = "serpentina_obs") val serpentinaObs: String = "",
    
    @Json(name = "bandeja_status") val bandejaStatus: String = "C",
    @Json(name = "bandeja_obs") val bandejaObs: String = "",
    
    @Json(name = "motor_status") val motorStatus: String = "C",
    @Json(name = "motor_obs") val motorObs: String = "",
    
    @Json(name = "eletrico_status") val eletricoStatus: String = "C",
    @Json(name = "eletrico_obs") val eletricoObs: String = "",
    
    @Json(name = "gas_volt_status") val gasVoltStatus: String = "C",
    @Json(name = "gas_volt_obs") val gasVoltObs: String = "",
    
    @Json(name = "isolamento_status") val isolamentoStatus: String = "C",
    @Json(name = "isolamento_obs") val isolamentoObs: String = "",
    
    @Json(name = "assinatura_digital_tecnico") val assinaturaDigitalTecnico: String = "", // Base64
    @Json(name = "assinatura_digital_cliente") val assinaturaDigitalCliente: String = "", // Base64
    @Json(name = "equipamentos_selecionados_ids") val equipamentosSelecionadosIds: String = "", // Comma-separated or JSON list of equipment IDs under control
    @Json(name = "rotinas_json") val rotinasJson: String = "", // JSON list of custom routines and periodicities
    @Json(name = "procedimentos_operacionais_json") val procedimentosOperacionaisJson: String = "", // JSON of customized operational procedures
    @Json(name = "procedimentos_emergenciais_json") val procedimentosEmergenciaisJson: String = "", // JSON of customized emergency procedures
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(
    tableName = "pmoc_logbook_entries",
    foreignKeys = [
        ForeignKey(
            entity = PmocReport::class,
            parentColumns = ["id"],
            childColumns = ["pmocId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PmocLogbookEntry(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "pmoc_id") val pmocId: String,
    @Json(name = "equipamento_id") val equipamentoId: String,
    @Json(name = "atividade") val atividade: String,
    @Json(name = "periodicidade") val periodicidade: String,
    @Json(name = "data_realizacao") val dataRealizacao: String, // e.g. "29/05/2026"
    @Json(name = "concluido") val concluido: Boolean
) : Serializable

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey 
    @Json(name = "email") val email: String,
    @Json(name = "nome") val nome: String,
    @Json(name = "usuario_id") val usuarioId: String,
    @Json(name = "premium_ativo") val premiumAtivo: Boolean = false,
    @Json(name = "plano_ativo") val planoAtivo: String = "FREE",
    @Json(name = "is_dark_mode") val isDarkMode: Boolean = false,
    @Json(name = "data_registro") val dataRegistro: String,
    @Json(name = "ultimo_acesso") val ultimoAcesso: String? = null,
    @Json(name = "plano_manual") val planoManual: String = ""
) : Serializable

@Entity(tableName = "pecas")
data class Peca(
    @PrimaryKey 
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "nome") val nome: String,
    @Json(name = "marca") val marca: String,
    @Json(name = "preco") val preco: Double,
    @Json(name = "detalhes") val detalhes: String = "",
    @Json(name = "usuario_email") val usuarioEmail: String = ""
) : Serializable

@Entity(
    tableName = "programas_preventivos",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProgramaPreventivo(
    @PrimaryKey
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "cliente_id") val clienteId: String,
    @Json(name = "ambiente_id") val ambienteId: String = "",
    @Json(name = "equipamento_id") val equipamentoId: String = "",
    @Json(name = "data_agendada") val dataAgendada: String, // format "dd/MM/yyyy"
    @Json(name = "periodo") val periodo: String, // "3 meses", "6 meses", "1 ano", "2 anos"
    @Json(name = "ordem_servico_original_id") val ordemServicoOriginalId: String = "",
    @Json(name = "ordem_servico_criada_id") val ordemServicoCriadaId: String = "",
    @Json(name = "ativo") val ativo: Boolean = true,
    @Json(name = "cancelado") val cancelado: Boolean = false,
    @Json(name = "notificar_cliente") val notificarCliente: Boolean = false,
    @Json(name = "notificar_app") val notificarApp: Boolean = true,
    @Json(name = "usuario_email") val usuarioEmail: String = "",
    @Json(name = "status") val status: String = "Pendente", // "Pendente" or "Concluído"
    @Json(name = "tecnico_id") val tecnicoId: String = "",
    @Json(name = "equipamentos_serialized") val equipamentosSerialized: String = "" // Format: "eq1|Pendente|;eq2|Concluído|10/06/2026"
) : Serializable

@Entity(tableName = "marcas_equipamentos")
data class MarcaEquipamento(
    @PrimaryKey
    @Json(name = "id") val id: String = generateRandomId(),
    @Json(name = "nome") val nome: String,
    @Json(name = "usuario_email") val usuarioEmail: String? = ""
) : Serializable


@Entity(tableName = "log_versao")
data class LogVersao(
    @PrimaryKey
    @Json(name = "id") val id: Int,
    @Json(name = "versao") val versao: String,
    @Json(name = "data") val data: String,
    @Json(name = "alteracoes") val alteracoes: String // Newline-separated items for easy grid-editing in Supabase
) : Serializable

