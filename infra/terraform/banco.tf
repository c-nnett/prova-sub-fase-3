locals {
  nome_do_banco    = "agendabeleza"
  usuario_do_banco = "agendabeleza"
}

# Gerada pelo Terraform e guardada só no estado (terraform.tfstate), que fica fora do Git.
resource "random_password" "banco" {
  length  = 32
  special = false
}

resource "docker_image" "postgres" {
  name = "postgres:16-alpine"
}

# Sem porta publicada: só a API, na mesma rede, chega ao banco.
resource "docker_container" "postgres" {
  name    = "postgres"
  image   = docker_image.postgres.image_id
  restart = "unless-stopped"
  wait    = true

  env = [
    "POSTGRES_DB=${local.nome_do_banco}",
    "POSTGRES_USER=${local.usuario_do_banco}",
    "POSTGRES_PASSWORD=${random_password.banco.result}",
    "TZ=America/Sao_Paulo",
  ]

  networks_advanced {
    name = docker_network.agendabeleza.name
  }

  volumes {
    volume_name    = docker_volume.postgres_dados.name
    container_path = "/var/lib/postgresql/data"
  }

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U ${local.usuario_do_banco} -d ${local.nome_do_banco}"]
    interval = "5s"
    timeout  = "5s"
    retries  = 10
  }
}
