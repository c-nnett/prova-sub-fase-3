-- Módulo profissionais: perfil, especialidades, serviços com tarifa, jornada semanal e bloqueios.
-- Modelagem: artefatos/modelagem-de-dados.md, seção 3.3.

CREATE TABLE profissional (
    id                 uuid          PRIMARY KEY,
    estabelecimento_id uuid          NOT NULL REFERENCES estabelecimento (id),
    -- Sem FK: o usuário é identificado pelo cabeçalho X-Usuario-Id (planejamento, seção 2.1).
    usuario_id         uuid,
    nome               varchar(120)  NOT NULL,
    email              varchar(180)  NOT NULL,
    bio                text,
    nota_media         numeric(3,2),
    total_avaliacoes   int           NOT NULL DEFAULT 0,
    ativo              boolean       NOT NULL DEFAULT true,
    criado_em          timestamptz   NOT NULL DEFAULT now(),
    atualizado_em      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT profissional_usuario_uk UNIQUE (usuario_id),
    CONSTRAINT profissional_nota_media_ck CHECK (nota_media BETWEEN 0 AND 5),
    CONSTRAINT profissional_total_avaliacoes_ck CHECK (total_avaliacoes >= 0)
);

CREATE INDEX profissional_estabelecimento_idx ON profissional (estabelecimento_id);

-- Catálogo comum a todos os estabelecimentos. Os ids são fixos para que clientes e testes possam referenciá-los.
CREATE TABLE especialidade (
    id   uuid         PRIMARY KEY,
    nome varchar(80)  NOT NULL,
    CONSTRAINT especialidade_nome_uk UNIQUE (nome)
);

INSERT INTO especialidade (id, nome) VALUES
    ('e5000000-0000-4000-8000-000000000001', 'Corte'),
    ('e5000000-0000-4000-8000-000000000002', 'Coloração'),
    ('e5000000-0000-4000-8000-000000000003', 'Escova'),
    ('e5000000-0000-4000-8000-000000000004', 'Manicure'),
    ('e5000000-0000-4000-8000-000000000005', 'Pedicure'),
    ('e5000000-0000-4000-8000-000000000006', 'Design de sobrancelhas'),
    ('e5000000-0000-4000-8000-000000000007', 'Maquiagem'),
    ('e5000000-0000-4000-8000-000000000008', 'Depilação'),
    ('e5000000-0000-4000-8000-000000000009', 'Massagem'),
    ('e5000000-0000-4000-8000-00000000000a', 'Barba');

CREATE TABLE profissional_especialidade (
    profissional_id  uuid  NOT NULL REFERENCES profissional (id) ON DELETE CASCADE,
    especialidade_id uuid  NOT NULL REFERENCES especialidade (id),
    PRIMARY KEY (profissional_id, especialidade_id)
);

CREATE TABLE profissional_servico (
    profissional_id uuid           NOT NULL REFERENCES profissional (id) ON DELETE CASCADE,
    servico_id      uuid           NOT NULL REFERENCES servico (id),
    tarifa          numeric(10,2)  NOT NULL,
    PRIMARY KEY (profissional_id, servico_id),
    CONSTRAINT profissional_servico_tarifa_ck CHECK (tarifa >= 0)
);

-- Usado pela busca por faixa de preço (modelagem, seção 5).
CREATE INDEX idx_profissional_servico_tarifa ON profissional_servico (servico_id, tarifa);

-- Jornada semanal recorrente. A sobreposição de períodos é validada no domínio: um EXCLUDE exigiria o
-- btree_gist, que entra na migração de agendamentos.
CREATE TABLE disponibilidade_profissional (
    id              uuid      PRIMARY KEY,
    profissional_id uuid      NOT NULL REFERENCES profissional (id) ON DELETE CASCADE,
    dia_semana      smallint  NOT NULL,
    hora_inicio     time      NOT NULL,
    hora_fim        time      NOT NULL,
    CONSTRAINT disponibilidade_dia_ck CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT disponibilidade_intervalo_ck CHECK (hora_fim > hora_inicio)
);

CREATE INDEX disponibilidade_profissional_dia_idx ON disponibilidade_profissional (profissional_id, dia_semana);

-- Ausências pontuais, como férias ou folga. O domínio e os endpoints entram com o cálculo de horários livres.
CREATE TABLE bloqueio_agenda (
    id              uuid          PRIMARY KEY,
    profissional_id uuid          NOT NULL REFERENCES profissional (id) ON DELETE CASCADE,
    inicio          timestamptz   NOT NULL,
    fim             timestamptz   NOT NULL,
    motivo          varchar(150),
    CONSTRAINT bloqueio_agenda_intervalo_ck CHECK (fim > inicio)
);

CREATE INDEX idx_bloqueio_prof_periodo ON bloqueio_agenda (profissional_id, inicio, fim);
