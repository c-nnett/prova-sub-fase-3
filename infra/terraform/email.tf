# Mailpit interno, como no Compose: recebe as confirmações e lembretes da outbox sem enviar nada para fora. O SMTP só
# existe na rede interna, e a interface web fica presa ao 127.0.0.1 da VPS: só abre por túnel SSH (README).
resource "docker_image" "mailpit" {
  name = "axllent/mailpit:latest"
}

resource "docker_container" "mailpit" {
  name    = "mailpit"
  image   = docker_image.mailpit.image_id
  restart = "unless-stopped"

  ports {
    internal = 8025
    external = 8025
    ip       = "127.0.0.1"
  }

  networks_advanced {
    name = docker_network.agendabeleza.name
  }
}
