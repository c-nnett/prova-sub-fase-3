-- Módulo calendario: assinatura do feed iCalendar (.ics) por URL secreta, uma por profissional e uma por cliente
-- (funcionalidade 7). Substitui integracao_calendario e evento_calendario do OAuth2, cortados em 20/11
-- (planejamento, seção 2.1). Modelagem: artefatos/modelagem-de-dados.md, seção 3.7.

CREATE TABLE assinatura_calendario (
    id          uuid         PRIMARY KEY,
    papel       varchar(20)  NOT NULL,
    -- profissional.id ou o cliente_id do agendamento. Sem FK: o cliente não tem tabela (identidade pelo
    -- X-Usuario-Id), e o mesmo campo atende aos dois papéis.
    titular_id  uuid         NOT NULL,
    -- SHA-256 do token em hexadecimal. O token em claro só aparece na resposta da emissão.
    token_hash  char(64)     NOT NULL,
    criada_em   timestamptz  NOT NULL,
    CONSTRAINT assinatura_calendario_papel_ck CHECK (papel IN ('CLIENTE', 'PROFISSIONAL')),
    -- Uma assinatura por titular: emitir de novo troca o token e revoga a URL anterior.
    CONSTRAINT assinatura_calendario_titular_uk UNIQUE (papel, titular_id),
    -- Também é o índice da leitura do feed pelo token.
    CONSTRAINT assinatura_calendario_token_uk UNIQUE (token_hash)
);
