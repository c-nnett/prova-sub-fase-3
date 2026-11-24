-- Ocupações do profissional em um período (horários livres, reserva e bloqueio).
-- Modelagem: artefatos/modelagem-de-dados.md, seção 5.
-- A consulta compara as colunas inicio e fim e recebe os status como parâmetro, então o GiST parcial da restrição
-- agendamento_sem_conflito, sobre tstzrange, não a atende.
CREATE INDEX idx_agendamento_prof_periodo ON agendamento (profissional_id, inicio, fim);
