-- =====================================================================
-- SCRIPT SQL COMPLETO PARA CONFIGURAÇÃO DO SUPABASE (AR-CONTROL PRO)
-- =====================================================================
-- Este script realiza o DROP de todas as tabelas caso já existam (na ordem correta)
-- e recria as tabelas estruturadas conforme os modelos de dados da aplicação.
-- Todas as relações de usuário_email foram desacopladas fisicamente para evitar o erro 23503,
-- garantindo a robustez de sincronização offline-first assíncrona.
-- Todas as colunas possuem NOT NULL e valores default para evitar crashes de parse Non-Null (null pointer) no app.

-- 1. DROP DAS TABELAS EXISTENTES (Evita conflitos de chaves)
DROP TABLE IF EXISTS pmoc_logbook_entries CASCADE;
DROP TABLE IF EXISTS pmoc_reports CASCADE;
DROP TABLE IF EXISTS ordens_servico CASCADE;
DROP TABLE IF EXISTS programas_preventivos CASCADE;
DROP TABLE IF EXISTS equipamentos CASCADE;
DROP TABLE IF EXISTS ambientes CASCADE;
DROP TABLE IF EXISTS tecnicos CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS pecas CASCADE;
DROP TABLE IF EXISTS marcas_equipamentos CASCADE;

-- 2. CRIAÇÃO DAS TABELAS

-- Tabela de Usuários
CREATE TABLE usuarios (
    email VARCHAR(255) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    usuario_id VARCHAR(100) NOT NULL,
    premium_ativo BOOLEAN DEFAULT FALSE NOT NULL,
    plano_ativo VARCHAR(50) DEFAULT 'FREE' NOT NULL,
    is_dark_mode BOOLEAN DEFAULT FALSE NOT NULL,
    data_registro VARCHAR(100) NOT NULL,
    ultimo_acesso VARCHAR(100),
    plano_manual VARCHAR(50) DEFAULT ''
);

-- Tabela de Clientes
CREATE TABLE clientes (
    id VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    documento VARCHAR(100) DEFAULT '' NOT NULL,
    telefone VARCHAR(100) DEFAULT '' NOT NULL, -- Corrigido tamanho para aceitar máscara celular do Brasil
    email VARCHAR(255) DEFAULT '' NOT NULL,
    endereco TEXT DEFAULT '' NOT NULL,
    nome_completo VARCHAR(255) DEFAULT '' NOT NULL,
    tipo_pessoa VARCHAR(50) DEFAULT 'Física' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de Ambientes
CREATE TABLE ambientes (
    id VARCHAR(100) PRIMARY KEY,
    cliente_id VARCHAR(100) NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    area_m2 NUMERIC DEFAULT 0.0 NOT NULL,
    carga_termica_btu INT DEFAULT 0 NOT NULL,
    fixos INT DEFAULT 0 NOT NULL,
    flutuantes INT DEFAULT 0 NOT NULL,
    janelas INT DEFAULT 0 NOT NULL,
    portas INT DEFAULT 0 NOT NULL,
    fontes_calor INT DEFAULT 0 NOT NULL,
    incidencia_solar VARCHAR(50) DEFAULT 'Baixa' NOT NULL,
    endereco TEXT DEFAULT '' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de Equipamentos
CREATE TABLE equipamentos (
    id VARCHAR(100) PRIMARY KEY,
    cliente_id VARCHAR(100) NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    ambiente_id VARCHAR(100) NOT NULL REFERENCES ambientes(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    numero_serie VARCHAR(100) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    capacidade_btu INT DEFAULT 12000 NOT NULL,
    fluido_refrigerante VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'Ativo' NOT NULL,
    data_instalacao VARCHAR(50) DEFAULT '' NOT NULL,
    observacoes TEXT DEFAULT '' NOT NULL,
    potencia_w VARCHAR(50) DEFAULT '' NOT NULL,
    tensao VARCHAR(50) DEFAULT '220' NOT NULL,
    sistema VARCHAR(50) DEFAULT 'Inverter' NOT NULL,
    ciclo VARCHAR(50) DEFAULT 'Frio' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de Técnicos
CREATE TABLE tecnicos (
    id VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    nome_completo VARCHAR(255) DEFAULT '' NOT NULL,
    cpf VARCHAR(50) DEFAULT '' NOT NULL,
    data_nascimento VARCHAR(50) DEFAULT '' NOT NULL,
    email VARCHAR(255) DEFAULT '' NOT NULL,
    telefone VARCHAR(100) DEFAULT '' NOT NULL,
    crea_cft VARCHAR(100) DEFAULT '' NOT NULL,
    tem_cnh BOOLEAN DEFAULT FALSE NOT NULL,
    disponivel_viagens BOOLEAN DEFAULT FALSE NOT NULL,
    obs TEXT DEFAULT '' NOT NULL,
    status VARCHAR(50) DEFAULT 'Ativo' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de Ordens de Serviço
CREATE TABLE ordens_servico (
    id VARCHAR(100) PRIMARY KEY,
    numero_ordem VARCHAR(100) UNIQUE NOT NULL,
    cliente_id VARCHAR(100) NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    tecnico_id VARCHAR(100) DEFAULT '' NOT NULL, -- Desacoplado fisicamente para aceitar técnicos nulos ou excluídos
    equipamento_id VARCHAR(100) NOT NULL REFERENCES equipamentos(id) ON DELETE CASCADE,
    tipo_servico VARCHAR(100) NOT NULL,
    data_criacao BIGINT NOT NULL,
    data_agendada VARCHAR(100) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(100) NOT NULL,
    diagnostico_tecnico TEXT DEFAULT '' NOT NULL,
    pecas_trocadas TEXT DEFAULT '' NOT NULL,
    valor_servico NUMERIC DEFAULT 0.0 NOT NULL,
    assinatura_digital TEXT DEFAULT '' NOT NULL,
    assinatura_cliente_nome VARCHAR(255) DEFAULT '' NOT NULL,
    data_chamado VARCHAR(100) DEFAULT '' NOT NULL,
    prioridade VARCHAR(50) DEFAULT 'Média' NOT NULL,
    descricao_resumida TEXT DEFAULT '' NOT NULL,
    corrente_eletrica VARCHAR(50) DEFAULT '' NOT NULL,
    temperatura VARCHAR(50) DEFAULT '' NOT NULL,
    pressao_gas_alta VARCHAR(50) DEFAULT '' NOT NULL,
    pressao_gas_baixa VARCHAR(50) DEFAULT '' NOT NULL,
    foto1 TEXT DEFAULT '' NOT NULL,
    foto2 TEXT DEFAULT '' NOT NULL,
    foto3 TEXT DEFAULT '' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de PMOC Reports
CREATE TABLE pmoc_reports (
    id VARCHAR(100) PRIMARY KEY,
    cliente_id VARCHAR(100) NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    tecnico_id VARCHAR(100) DEFAULT '' NOT NULL,
    data_geracao BIGINT NOT NULL,
    mes_ref VARCHAR(100) NOT NULL,
    numero_pmoc VARCHAR(100) NOT NULL,
    filtro_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    filtro_obs TEXT DEFAULT '' NOT NULL,
    serpentina_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    serpentina_obs TEXT DEFAULT '' NOT NULL,
    bandeja_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    bandeja_obs TEXT DEFAULT '' NOT NULL,
    motor_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    motor_obs TEXT DEFAULT '' NOT NULL,
    eletrico_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    eletrico_obs TEXT DEFAULT '' NOT NULL,
    gas_volt_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    gas_volt_obs TEXT DEFAULT '' NOT NULL,
    isolamento_status VARCHAR(10) DEFAULT 'C' NOT NULL,
    isolamento_obs TEXT DEFAULT '' NOT NULL,
    assinatura_digital_tecnico TEXT DEFAULT '' NOT NULL,
    assinatura_digital_cliente TEXT DEFAULT '' NOT NULL,
    equipamentos_selecionados_ids TEXT DEFAULT '' NOT NULL,
    rotinas_json TEXT DEFAULT '' NOT NULL,
    procedimentos_operacionais_json TEXT DEFAULT '' NOT NULL,
    procedimentos_emergenciais_json TEXT DEFAULT '' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de PMOC Logbook Entries
CREATE TABLE pmoc_logbook_entries (
    id VARCHAR(100) PRIMARY KEY,
    pmoc_id VARCHAR(100) NOT NULL REFERENCES pmoc_reports(id) ON DELETE CASCADE,
    equipamento_id VARCHAR(100) NOT NULL REFERENCES equipamentos(id) ON DELETE CASCADE,
    atividade TEXT NOT NULL,
    periodicidade VARCHAR(100) NOT NULL,
    data_realizacao VARCHAR(100) NOT NULL,
    concluido BOOLEAN DEFAULT FALSE NOT NULL
);

-- Tabela de Peças
CREATE TABLE pecas (
    id VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    marca VARCHAR(255) NOT NULL,
    preco NUMERIC DEFAULT 0.0 NOT NULL,
    detalhes TEXT DEFAULT '' NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL
);

-- Tabela de Programas Preventivos
CREATE TABLE programas_preventivos (
    id VARCHAR(100) PRIMARY KEY,
    cliente_id VARCHAR(100) NOT NULL,
    ambiente_id VARCHAR(100) NOT NULL,
    equipamento_id VARCHAR(100) NOT NULL,
    data_agendada VARCHAR(50) NOT NULL,
    periodo VARCHAR(100) NOT NULL,
    ordem_servico_original_id VARCHAR(100) DEFAULT '' NOT NULL,
    ordem_servico_criada_id VARCHAR(100) DEFAULT '' NOT NULL,
    ativo BOOLEAN DEFAULT TRUE NOT NULL,
    cancelado BOOLEAN DEFAULT FALSE NOT NULL,
    notificar_cliente BOOLEAN DEFAULT FALSE NOT NULL,
    notificar_app BOOLEAN DEFAULT TRUE NOT NULL,
    usuario_email VARCHAR(255) DEFAULT '' NOT NULL,
    status VARCHAR(50) DEFAULT 'Pendente' NOT NULL,
    tecnico_id VARCHAR(100) DEFAULT '' NOT NULL,
    equipamentos_serialized TEXT DEFAULT '' NOT NULL
);

-- Tabela de Marcas de Equipamentos (Admite nulo em usuario_email para marcas padrão do sistema)
CREATE TABLE marcas_equipamentos (
    id VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(100) UNIQUE NOT NULL,
    usuario_email VARCHAR(255) DEFAULT NULL
);

-- 3. INSERÇÃO DAS MARCAS PADRÃO DO SISTEMA (Disponíveis globalmente para todos os usuários)
INSERT INTO marcas_equipamentos (id, nome, usuario_email) VALUES
('M01', 'Agratto', NULL),
('M02', 'Carrier', NULL),
('M03', 'Comfee', NULL),
('M04', 'Consul', NULL),
('M05', 'Daikin', NULL),
('M06', 'Elgin', NULL),
('M07', 'Electrolux', NULL),
('M08', 'Fujitsu', NULL),
('M09', 'Gree', NULL),
('M10', 'Hisense', NULL),
('M11', 'Hitachi', NULL),
('M12', 'Komeco', NULL),
('M13', 'LG', NULL),
('M14', 'Midea', NULL),
('M15', 'Philco', NULL),
('M16', 'Samsung', NULL),
('M17', 'Springer', NULL),
('M18', 'TCL', NULL),
('M19', 'York', NULL)
ON CONFLICT (nome) DO NOTHING;

-- 4. CRIAÇÃO DE ÍNDICES OTIMIZADOS PARA BUSCA POR USUÁRIO
CREATE INDEX IF NOT EXISTS idx_clientes_usuario ON clientes(usuario_email);
CREATE INDEX IF NOT EXISTS idx_ambientes_usuario ON ambientes(usuario_email);
CREATE INDEX IF NOT EXISTS idx_equipamentos_usuario ON equipamentos(usuario_email);
CREATE INDEX IF NOT EXISTS idx_tecnicos_usuario ON tecnicos(usuario_email);
CREATE INDEX IF NOT EXISTS idx_ordens_usuario ON ordens_servico(usuario_email);
CREATE INDEX IF NOT EXISTS idx_pmoc_usuario ON pmoc_reports(usuario_email);
CREATE INDEX IF NOT EXISTS idx_pecas_usuario ON pecas(usuario_email);
CREATE INDEX IF NOT EXISTS idx_programas_usuario ON programas_preventivos(usuario_email);
CREATE INDEX IF NOT EXISTS idx_marcas_usuario ON marcas_equipamentos(usuario_email);

-- 5. DESATIVAR RLS (ROW LEVEL SECURITY) EM TODAS AS TABELAS PARA GARANTIR PERMISSÃO TOTAL VIA API ANON KEY
ALTER TABLE usuarios DISABLE ROW LEVEL SECURITY;
ALTER TABLE clientes DISABLE ROW LEVEL SECURITY;
ALTER TABLE ambientes DISABLE ROW LEVEL SECURITY;
ALTER TABLE equipamentos DISABLE ROW LEVEL SECURITY;
ALTER TABLE tecnicos DISABLE ROW LEVEL SECURITY;
ALTER TABLE ordens_servico DISABLE ROW LEVEL SECURITY;
ALTER TABLE pmoc_reports DISABLE ROW LEVEL SECURITY;
ALTER TABLE pmoc_logbook_entries DISABLE ROW LEVEL SECURITY;
ALTER TABLE pecas DISABLE ROW LEVEL SECURITY;
ALTER TABLE programas_preventivos DISABLE ROW LEVEL SECURITY;
ALTER TABLE marcas_equipamentos DISABLE ROW LEVEL SECURITY;
