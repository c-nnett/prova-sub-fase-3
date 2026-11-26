# language: pt
Funcionalidade: Avaliação de atendimento
  Como cliente de um salão
  Quero avaliar o atendimento que recebi
  Para que outros clientes conheçam a qualidade do salão e da profissional

  Contexto:
    Dado um salão com a profissional "Ana", que atende às segundas das 09:00 às 18:00
    E que fui atendido por ela e o gestor concluiu o atendimento

  Cenário: Cliente avalia o atendimento e as notas médias mudam
    Quando avalio o atendimento com nota 5 para o salão, 4 para a profissional e o comentário "Corte impecável"
    Então a avaliação é registrada
    E o salão tem nota média "5.00" em 1 avaliação
    E a profissional tem nota média "4.00" em 1 avaliação
    E a lista pública de avaliações do salão mostra o comentário "Corte impecável"

  Cenário: Segunda avaliação do mesmo atendimento é recusada
    Dado que já avaliei o atendimento com nota 5 para o salão e 5 para a profissional
    Quando avalio o atendimento com nota 1 para o salão, 1 para a profissional e o comentário "Mudei de ideia"
    Então a avaliação é recusada com status 422 e a mensagem "Este atendimento já foi avaliado"
    E o salão tem nota média "5.00" em 1 avaliação
    E a profissional tem nota média "5.00" em 1 avaliação
