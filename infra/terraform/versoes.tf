# Deploy da AgendaBeleza API numa VPS Linux da HostGator que já existe. O Terraform não cria a máquina: fala com o
# Docker dela por SSH e descreve os contêineres, como uma task definition do ECS (arquitetura, seção 11).
terraform {
  required_version = ">= 1.9"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.7"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}

# Na VPS (deploy automático, aplicar.sh), fala com o Docker pelo socket local. Fora dela, usa o cliente ssh do
# sistema: a chave precisa entrar sem senha interativa (README, pré-requisitos).
provider "docker" {
  host     = var.docker_host != "" ? var.docker_host : "ssh://${var.acesso_ssh}"
  ssh_opts = var.docker_host != "" ? [] : concat(["-o", "StrictHostKeyChecking=accept-new"], var.chave_ssh == "" ? [] : ["-i", var.chave_ssh])
}
