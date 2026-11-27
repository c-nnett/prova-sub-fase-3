#!/bin/sh
# Aplica o Terraform na própria VPS, onde ficam o estado e o terraform.tfvars (fora do Git). O deploy automático
# (.github/workflows/deploy.yml) chama este script por SSH; para um deploy manual, rode-o na VPS como o usuário deploy.
#
#   ./aplicar.sh <versao>   publica a imagem ghcr.io/c-nnett/agendabeleza-api:<versao> (sha curto do commit)
#   ./aplicar.sh            reaplica a versão em uso, para mudanças só de infraestrutura
#
# O Terraform roda num contêiner: nada é instalado na VPS. Ele fala com o Docker pelo socket local
# (docker_host = "unix:///var/run/docker.sock" no terraform.tfvars).
set -eu

DIRETORIO=$(cd "$(dirname "$0")" && pwd)
IMAGEM_DO_TERRAFORM=hashicorp/terraform:1.17.2

# Roda com o usuário de quem chamou (deploy), para o estado e o .terraform/ não ficarem com dono root, e com o grupo
# dono do socket, para falar com o Docker.
terraform() {
	docker run --rm \
		--user "$(id -u):$(id -g)" \
		--group-add "$(stat -c %g /var/run/docker.sock)" \
		-e HOME=/tmp \
		-v "$DIRETORIO":/infra \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-w /infra \
		"$IMAGEM_DO_TERRAFORM" "$@"
}

terraform init -input=false -no-color

VERSAO=${1:-}
if [ -z "$VERSAO" ]; then
	VERSAO=$(terraform output -raw versao_publicada | sed 's/.*://')
	echo "Sem versão informada: reaplicando a versão em uso, $VERSAO."
fi

terraform apply -input=false -auto-approve -no-color -var "versao_da_imagem=$VERSAO"
