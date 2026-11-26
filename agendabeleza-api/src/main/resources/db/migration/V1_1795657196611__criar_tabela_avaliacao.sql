-- Módulo avaliações: uma avaliação por atendimento concluído, com notas para o estabelecimento e o profissional.
-- Modelagem: artefatos/modelagem-de-dados.md, seções 3.5 e 4.3. As colunas nota_media e total_avaliacoes de
-- estabelecimento e profissional já existem e são recalculadas na mesma transação de cada avaliação.

CREATE TABLE avaliacao (
    id                   uuid           PRIMARY KEY,
    -- Única: um atendimento recebe no máximo uma avaliação.
    agendamento_id       uuid           NOT NULL UNIQUE REFERENCES agendamento (id),
    -- Redundância proposital para as listagens públicas sem junção com agendamento.
    estabelecimento_id   uuid           NOT NULL REFERENCES estabelecimento (id),
    profissional_id      uuid           NOT NULL REFERENCES profissional (id),
    -- Sem FK: o usuário é identificado pelo cabeçalho X-Usuario-Id (planejamento, seção 2.1).
    cliente_id           uuid           NOT NULL,
    nota_estabelecimento smallint       NOT NULL,
    nota_profissional    smallint       NOT NULL,
    comentario           varchar(1000),
    criada_em            timestamptz    NOT NULL,
    CONSTRAINT avaliacao_nota_estabelecimento_ck CHECK (nota_estabelecimento BETWEEN 1 AND 5),
    CONSTRAINT avaliacao_nota_profissional_ck CHECK (nota_profissional BETWEEN 1 AND 5)
);

-- Listagens públicas, da mais recente para a mais antiga (modelagem, seção 5).
CREATE INDEX idx_avaliacao_estab ON avaliacao (estabelecimento_id, criada_em DESC);
CREATE INDEX idx_avaliacao_prof ON avaliacao (profissional_id, criada_em DESC);
