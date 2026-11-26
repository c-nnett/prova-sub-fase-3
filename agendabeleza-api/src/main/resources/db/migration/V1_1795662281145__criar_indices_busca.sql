-- Módulo busca: índices da busca de estabelecimentos (funcionalidade 5).
-- Modelagem: artefatos/modelagem-de-dados.md, seção 5. Sem pg_trgm (planejamento, seção 2.1): o nome é buscado
-- por trecho com LIKE, e a faixa de preço usa o idx_profissional_servico_tarifa, criado com os profissionais.

-- Cidade e bairro são comparados sem diferenciar maiúsculas, com a mesma expressão do índice.
CREATE INDEX idx_estabelecimento_cidade_bairro ON estabelecimento (lower(cidade), lower(bairro)) WHERE ativo;

-- Ordem padrão da busca: melhor avaliados primeiro, sem avaliação no fim.
CREATE INDEX idx_estabelecimento_nota ON estabelecimento (nota_media DESC NULLS LAST) WHERE ativo;

-- Filtro por categoria dentro dos serviços ativos de cada estabelecimento.
CREATE INDEX idx_servico_categoria ON servico (estabelecimento_id, categoria) WHERE ativo;
