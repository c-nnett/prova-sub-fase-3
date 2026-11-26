# Rede própria: os contêineres se encontram pelo nome (postgres, mailpit, agendabeleza-api), e só o proxy publica
# portas na VPS.
resource "docker_network" "agendabeleza" {
  name = "agendabeleza"
}

# Os volumes sobrevivem à troca de contêineres. O do Caddy guarda os certificados: sem ele, cada recriação pediria um
# certificado novo e esbarraria no limite de emissões do Let's Encrypt.
resource "docker_volume" "postgres_dados" {
  name = "agendabeleza-postgres-dados"
}

resource "docker_volume" "caddy_dados" {
  name = "agendabeleza-caddy-dados"
}

resource "docker_volume" "caddy_configuracao" {
  name = "agendabeleza-caddy-configuracao"
}
