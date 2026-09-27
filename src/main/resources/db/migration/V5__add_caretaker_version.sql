-- Versao da linha do responsavel, para controle otimista de concorrencia (T285).
-- Uma escrita que partiu de uma copia vencida e recusada pelo banco, e o comando roda de novo
-- sobre o estado atual. Sem ela, uma edicao simultanea gravava por cima de uma inativacao e o
-- responsavel voltava a ativa.
ALTER TABLE caretaker ADD COLUMN version bigint NOT NULL DEFAULT 0;
