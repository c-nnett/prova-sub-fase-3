output "url_base_da_api" {
  description = "Prefixo das rotas da API."
  value       = "https://${var.dominio_da_api}/api/v1"
}

output "url_swagger" {
  description = "Documentação interativa da API."
  value       = "https://${var.dominio_da_api}/swagger-ui.html"
}

output "url_saude" {
  description = "Health check do Actuator."
  value       = "https://${var.dominio_da_api}/actuator/health"
}

output "versao_publicada" {
  description = "Imagem em execução."
  value       = docker_image.api.name
}
