-- Módulo notificacoes: outbox de e-mails, gravada na mesma transação da alteração do agendamento e enviada depois
-- por um job. Modelagem: artefatos/modelagem-de-dados.md, seção 3.6.

CREATE TABLE notificacao (
    id                 uuid          PRIMARY KEY,
    agendamento_id     uuid          NOT NULL REFERENCES agendamento (id) ON DELETE CASCADE,
    tipo               varchar(20)   NOT NULL,
    -- Quem recebe. O e-mail do cliente só existe aqui (não há tabela de usuários), e os avisos seguintes do mesmo
    -- agendamento o leem da notificação de papel CLIENTE.
    papel              varchar(20)   NOT NULL,
    destinatario_email varchar(180)  NOT NULL,
    assunto            varchar(200)  NOT NULL,
    corpo              text          NOT NULL,
    enviar_em          timestamptz   NOT NULL,
    status             varchar(20)   NOT NULL,
    tentativas         int           NOT NULL DEFAULT 0,
    enviada_em         timestamptz,
    ultimo_erro        text,
    CONSTRAINT notificacao_tipo_ck
        CHECK (tipo IN ('CONFIRMACAO', 'LEMBRETE', 'REAGENDAMENTO', 'CANCELAMENTO')),
    CONSTRAINT notificacao_papel_ck CHECK (papel IN ('CLIENTE', 'PROFISSIONAL')),
    CONSTRAINT notificacao_status_ck CHECK (status IN ('PENDENTE', 'ENVIADA', 'FALHA', 'CANCELADA')),
    CONSTRAINT notificacao_tentativas_ck CHECK (tentativas >= 0)
);

-- Rodada do job: pendentes vencidas em ordem de envio (modelagem, seção 5).
CREATE INDEX idx_notificacao_pendente ON notificacao (enviar_em) WHERE status = 'PENDENTE';
-- E-mail do cliente para os avisos de cancelamento e reagendamento.
CREATE INDEX idx_notificacao_agendamento_papel ON notificacao (agendamento_id, papel);
