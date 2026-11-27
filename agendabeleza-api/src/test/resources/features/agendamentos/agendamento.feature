# language: pt
Funcionalidade: Agendamento de serviços
  Como cliente de um salão
  Quero reservar um horário livre com a profissional
  Para garantir meu atendimento sem conflito com outros clientes

  Contexto:
    Dado um salão com a profissional "Ana", que atende às segundas das 09:00 às 18:00

  Cenário: Cliente reserva um horário livre e ele deixa de aparecer
    Dado que o horário de "2030-03-11T10:00:00-03:00" está livre
    Quando reservo o horário de "2030-03-11T10:00:00-03:00"
    Então o agendamento é criado com status "AGENDADO"
    E o horário de "2030-03-11T10:00:00-03:00" não aparece entre os horários livres

  Cenário: Dois clientes tentam reservar o mesmo horário ao mesmo tempo
    Quando dois clientes solicitam o horário de "2030-03-11T14:00:00-03:00" ao mesmo tempo
    Então exatamente uma reserva é criada com status 201
    E a outra é recusada com status 409 e a mensagem "O profissional já tem um agendamento nesse horário"

  Cenário: Reserva fora do atendimento da profissional é recusada
    Quando reservo o horário de "2030-03-11T19:00:00-03:00"
    Então a reserva é recusada com status 422 e a mensagem "O horário está fora do atendimento do profissional"

  Cenário: Cancelamento libera o horário para outro cliente
    Dado que reservei o horário de "2030-03-11T11:00:00-03:00" e o gestor confirmou
    Quando cancelo o agendamento com o motivo "Imprevisto"
    Então o agendamento fica com status "CANCELADO"
    E o horário de "2030-03-11T11:00:00-03:00" volta a aparecer entre os horários livres
    E outro cliente consegue reservar o horário de "2030-03-11T11:00:00-03:00"
