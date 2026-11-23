# language: pt
Funcionalidade: Cadastro de estabelecimento
  Como gestor de um salão
  Quero cadastrar meu estabelecimento, os horários, os serviços e as fotos
  Para que os clientes possam encontrá-lo e agendar

  Contexto:
    Dado que sou um gestor identificado

  Cenário: Gestor cadastra o estabelecimento e o cliente consulta os dados
    Quando cadastro o estabelecimento "Salão Bela Vista" em "São Paulo", bairro "Consolação", com os horários:
      | dia      | abre  | fecha |
      | MONDAY   | 09:00 | 18:00 |
      | SATURDAY | 08:00 | 14:00 |
    Então a resposta tem status 201
    E a resposta informa o endereço do novo estabelecimento
    E a consulta pública mostra o nome "Salão Bela Vista", 2 dias de funcionamento e o fuso "America/Sao_Paulo"

  Cenário: Gestor cadastra um serviço e ele aparece na lista pública
    Dado que cadastrei o estabelecimento "Salão Bela Vista"
    Quando cadastro o serviço "Corte feminino" da categoria "CABELO" com 60 minutos por "80.00" reais
    Então a resposta tem status 201
    E a lista pública de serviços contém "Corte feminino" por "80.00" reais

  Cenário: Gestor adiciona uma foto pela URL
    Dado que cadastrei o estabelecimento "Salão Bela Vista"
    Quando adiciono a foto "https://exemplo.com/fachada.jpg"
    Então a resposta tem status 201
    E a consulta pública mostra 1 foto

  Esquema do Cenário: Cadastro com dados inválidos é recusado
    Quando tento cadastrar um estabelecimento com <campo> igual a "<valor>"
    Então a resposta tem status <status>

    Exemplos:
      | campo | valor     | status |
      | nome  |           | 400    |
      | cep   | 123       | 422    |
      | uf    | São Paulo | 422    |

  Cenário: Outro usuário não altera o estabelecimento
    Dado que cadastrei o estabelecimento "Salão Bela Vista"
    Quando outro usuário tenta alterar o nome para "Invasão"
    Então a resposta tem status 403
    E a consulta pública continua mostrando o nome "Salão Bela Vista"
