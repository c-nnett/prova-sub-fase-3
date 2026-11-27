variable "acesso_ssh" {
  description = "Usuário e endereço da VPS, no formato usuario@ip ou usuario@ip:porta (SSH fora da 22). O usuário precisa estar no grupo docker."
  type        = string
}

variable "chave_ssh" {
  description = "Caminho da chave privada SSH. Vazio usa a configuração padrão do cliente ssh (~/.ssh/config)."
  type        = string
  default     = ""
}

variable "dominio_da_api" {
  description = "Domínio que aponta para a VPS, com registro A para o IP da VPS, por exemplo api.seudominio.com.br."
  type        = string
}

variable "email_do_certificado" {
  description = "E-mail de contato da conta Let's Encrypt usada pelo Caddy para emitir o certificado HTTPS."
  type        = string
}

variable "imagem_da_api" {
  description = "Repositório da imagem publicada pelo workflow imagem.yml."
  type        = string
  default     = "ghcr.io/c-nnett/agendabeleza-api"
}

variable "versao_da_imagem" {
  description = "Tag da imagem: o sha curto do commit publicado (git rev-parse --short HEAD)."
  type        = string
}

variable "memoria_da_api_em_mb" {
  description = "Limite de memória do contêiner da API. A JVM usa 75% dele (JAVA_OPTS do Dockerfile)."
  type        = number
  default     = 1024
}
