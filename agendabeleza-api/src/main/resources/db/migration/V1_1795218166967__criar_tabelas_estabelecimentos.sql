-- Módulo estabelecimentos: dados cadastrais, fotos, horários de funcionamento e serviços.
-- Modelagem: artefatos/modelagem-de-dados.md, seção 3.2.

CREATE TABLE estabelecimento (
    id               uuid          PRIMARY KEY,
    -- Sem FK: o usuário é identificado pelo cabeçalho X-Usuario-Id (planejamento, seção 2.1).
    gestor_id        uuid          NOT NULL,
    nome             varchar(150)  NOT NULL,
    descricao        text,
    logradouro       varchar(150)  NOT NULL,
    numero           varchar(10)   NOT NULL,
    complemento      varchar(80),
    bairro           varchar(80)   NOT NULL,
    cidade           varchar(80)   NOT NULL,
    uf               char(2)       NOT NULL,
    cep              char(8)       NOT NULL,
    timezone         varchar(50)   NOT NULL DEFAULT 'America/Sao_Paulo',
    nota_media       numeric(3,2),
    total_avaliacoes int           NOT NULL DEFAULT 0,
    ativo            boolean       NOT NULL DEFAULT true,
    criado_em        timestamptz   NOT NULL DEFAULT now(),
    atualizado_em    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT estabelecimento_nota_media_ck CHECK (nota_media BETWEEN 0 AND 5),
    CONSTRAINT estabelecimento_total_avaliacoes_ck CHECK (total_avaliacoes >= 0)
);

CREATE INDEX estabelecimento_gestor_idx ON estabelecimento (gestor_id);

-- As unicidades abaixo são adiadas para o commit: o Hibernate grava as atualizações antes das remoções,
-- e reordenar fotos ou trocar o horário de um dia passaria por estados intermediários repetidos.

CREATE TABLE foto_estabelecimento (
    id                 uuid          PRIMARY KEY,
    estabelecimento_id uuid          NOT NULL REFERENCES estabelecimento (id) ON DELETE CASCADE,
    url                varchar(500)  NOT NULL,
    ordem              int           NOT NULL,
    CONSTRAINT foto_estabelecimento_ordem_uk UNIQUE (estabelecimento_id, ordem) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT foto_estabelecimento_ordem_ck CHECK (ordem > 0)
);

CREATE TABLE horario_funcionamento (
    id                 uuid      PRIMARY KEY,
    estabelecimento_id uuid      NOT NULL REFERENCES estabelecimento (id) ON DELETE CASCADE,
    dia_semana         smallint  NOT NULL,
    abre_as            time      NOT NULL,
    fecha_as           time      NOT NULL,
    CONSTRAINT horario_funcionamento_dia_uk UNIQUE (estabelecimento_id, dia_semana) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT horario_funcionamento_dia_ck CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT horario_funcionamento_intervalo_ck CHECK (fecha_as > abre_as)
);

CREATE TABLE servico (
    id                 uuid           PRIMARY KEY,
    estabelecimento_id uuid           NOT NULL REFERENCES estabelecimento (id),
    nome               varchar(120)   NOT NULL,
    descricao          text,
    categoria          varchar(40)    NOT NULL,
    duracao_minutos    int            NOT NULL,
    preco_base         numeric(10,2)  NOT NULL,
    ativo              boolean        NOT NULL DEFAULT true,
    criado_em          timestamptz    NOT NULL DEFAULT now(),
    atualizado_em      timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT servico_categoria_ck
        CHECK (categoria IN ('CABELO', 'UNHAS', 'ESTETICA', 'MASSAGEM', 'BARBA', 'OUTROS')),
    CONSTRAINT servico_duracao_ck CHECK (duracao_minutos > 0),
    CONSTRAINT servico_preco_ck CHECK (preco_base >= 0)
);

CREATE INDEX servico_estabelecimento_idx ON servico (estabelecimento_id);
