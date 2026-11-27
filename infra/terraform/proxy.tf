# Caddy: única porta pública da VPS. Emite e renova o certificado Let's Encrypt do domínio sozinho e repassa para a
# API com os cabeçalhos X-Forwarded-*; com server.forward-headers-strategy=framework, a URL do feed .ics sai em
# https://<dominio>.
resource "docker_image" "caddy" {
  name = "caddy:2-alpine"
}

resource "docker_container" "caddy" {
  name    = "caddy"
  image   = docker_image.caddy.image_id
  restart = "unless-stopped"

  ports {
    internal = 80
    external = 80
  }

  ports {
    internal = 443
    external = 443
  }

  upload {
    file = "/etc/caddy/Caddyfile"
    content = templatefile("${path.module}/Caddyfile.tftpl", {
      dominio              = var.dominio_da_api
      email_do_certificado = var.email_do_certificado
      endereco_da_api      = "${docker_container.api.name}:8080"
    })
  }

  networks_advanced {
    name = docker_network.agendabeleza.name
  }

  volumes {
    volume_name    = docker_volume.caddy_dados.name
    container_path = "/data"
  }

  volumes {
    volume_name    = docker_volume.caddy_configuracao.name
    container_path = "/config"
  }
}
