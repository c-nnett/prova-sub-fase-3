# A imagem vem do GHCR, publicada pelo workflow imagem.yml. Trocar versao_da_imagem recria só este contêiner.
resource "docker_image" "api" {
  name = "${var.imagem_da_api}:${var.versao_da_imagem}"
}

# Equivalente à task definition do ECS: a configuração chega por variáveis de ambiente, e o perfil production
# (application-production.properties) traz o que é fixo da nuvem. O Spring liga SPRING_DATASOURCE_URL a
# spring.datasource.url, e assim por diante, sem mudar código.
resource "docker_container" "api" {
  name    = "agendabeleza-api"
  image   = docker_image.api.image_id
  restart = "unless-stopped"
  memory  = var.memoria_da_api_em_mb

  # Espera o HEALTHCHECK do Dockerfile (/actuator/health). A JVM e o Flyway levam cerca de um minuto para subir.
  wait         = true
  wait_timeout = 240

  env = [
    "SPRING_PROFILES_ACTIVE=production",
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${docker_container.postgres.name}:5432/${local.nome_do_banco}",
    "SPRING_DATASOURCE_USERNAME=${local.usuario_do_banco}",
    "SPRING_DATASOURCE_PASSWORD=${random_password.banco.result}",
    "SPRING_MAIL_HOST=${docker_container.mailpit.name}",
    "SPRING_MAIL_PORT=1025",
    "SPRING_OUTPUT_ANSI_ENABLED=never",
  ]

  networks_advanced {
    name = docker_network.agendabeleza.name
  }

  # Com memory definido, o Docker preenche o swap com o dobro do limite, e o provider acusaria essa diferença em todo
  # plan. A VPS não tem swap, então o valor não muda nada.
  lifecycle {
    ignore_changes = [memory_swap]
  }
}
