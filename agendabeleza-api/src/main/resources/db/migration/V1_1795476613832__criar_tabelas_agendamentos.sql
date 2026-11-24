-- Módulo agendamentos: reservas e trilha de alterações, com a restrição contra conflito de horário.
-- Modelagem: artefatos/modelagem-de-dados.md, seções 3.4 e 4.1.

-- Permite combinar igualdade de uuid (btree) e sobreposição de intervalo (gist) no mesmo EXCLUDE.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE agendamento (
    id                 uuid           PRIMARY KEY,
    -- Sem FK: o usuário é identificado pelo cabeçalho X-Usuario-Id (planejamento, seção 2.1).
    cliente_id         uuid           NOT NULL,
    estabelecimento_id uuid           NOT NULL REFERENCES estabelecimento (id),
    -- FKs separadas, e não composta para profissional_servico: o adaptador de profissionais regrava essa tabela
    -- a cada atualização. O vínculo profissional-serviço é validado no caso de uso.
    profissional_id    uuid           NOT NULL REFERENCES profissional (id),
    servico_id         uuid           NOT NULL REFERENCES servico (id),
    inicio             timestamptz    NOT NULL,
    fim                timestamptz    NOT NULL,
    status             varchar(20)    NOT NULL,
    preco              numeric(10,2)  NOT NULL,
    observacoes        varchar(500),
    versao             bigint         NOT NULL,
    criado_em          timestamptz    NOT NULL DEFAULT now(),
    atualizado_em      timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT agendamento_intervalo_ck CHECK (fim > inicio),
    CONSTRAINT agendamento_status_ck
        CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'CONCLUIDO', 'CANCELADO', 'NAO_COMPARECEU')),
    CONSTRAINT agendamento_preco_ck CHECK (preco >= 0),
    -- Um profissional não tem dois agendamentos ativos sobrepostos. O intervalo [) deixa um atendimento começar
    -- no instante em que o anterior termina. A violação gera o SQLState 23P01, traduzido para HTTP 409.
    CONSTRAINT agendamento_sem_conflito EXCLUDE USING gist (
        profissional_id WITH =,
        tstzrange(inicio, fim, '[)') WITH &&
    ) WHERE (status IN ('AGENDADO', 'CONFIRMADO'))
);

-- Painel do estabelecimento e "meus agendamentos" (modelagem, seção 5).
CREATE INDEX idx_agendamento_estab_inicio ON agendamento (estabelecimento_id, inicio);
CREATE INDEX idx_agendamento_cliente_inicio ON agendamento (cliente_id, inicio DESC);

-- Trilha de alterações, somente inserção. Uma linha por reserva ou mudança de status.
CREATE TABLE agendamento_historico (
    id              uuid          PRIMARY KEY,
    agendamento_id  uuid          NOT NULL REFERENCES agendamento (id) ON DELETE CASCADE,
    status_anterior varchar(20),
    status_novo     varchar(20)   NOT NULL,
    inicio_anterior timestamptz,
    inicio_novo     timestamptz,
    motivo          varchar(250),
    -- Sem FK, pelo mesmo motivo de agendamento.cliente_id.
    alterado_por    uuid          NOT NULL,
    ocorrido_em     timestamptz   NOT NULL,
    CONSTRAINT agendamento_historico_status_anterior_ck
        CHECK (status_anterior IN ('AGENDADO', 'CONFIRMADO', 'CONCLUIDO', 'CANCELADO', 'NAO_COMPARECEU')),
    CONSTRAINT agendamento_historico_status_novo_ck
        CHECK (status_novo IN ('AGENDADO', 'CONFIRMADO', 'CONCLUIDO', 'CANCELADO', 'NAO_COMPARECEU'))
);

CREATE INDEX idx_agendamento_historico_agendamento ON agendamento_historico (agendamento_id, ocorrido_em);
