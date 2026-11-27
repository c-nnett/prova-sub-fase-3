# Deploy na VPS (Terraform)

Publica a AgendaBeleza API numa VPS Linux da HostGator que já existe. O Terraform não cria a máquina: fala com o
Docker dela por SSH (provider `kreuzwerker/docker`) e descreve os contêineres, como uma task definition do ECS
descreveria na AWS.

```text
Internet ──443──▶ caddy (HTTPS Let's Encrypt) ──▶ agendabeleza-api:8080 ──▶ postgres:5432
                                                              └────────▶ mailpit:1025 (interno)
```

| Contêiner | Imagem | Porta pública | Dados |
|---|---|---|---|
| `caddy` | `caddy:2-alpine` | 80 e 443 | volumes `agendabeleza-caddy-*` (certificados) |
| `agendabeleza-api` | `ghcr.io/c-nnett/agendabeleza-api:<sha>` | nenhuma | — |
| `postgres` | `postgres:16-alpine` | nenhuma | volume `agendabeleza-postgres-dados` |
| `mailpit` | `axllent/mailpit` | só 8025 em `127.0.0.1` (túnel SSH) | — |

## Configuração por ambiente

| Onde | O quê |
|---|---|
| Perfil `production` (`application-production.properties`, dentro da imagem) | O que é fixo da nuvem: nível de log, detalhe da saúde oculto |
| Variáveis do contêiner (`api.tf`) | Endereços que só o deploy conhece: banco, SMTP, perfil ativo |
| Estado do Terraform (`/opt/agendabeleza/terraform/terraform.tfstate` na VPS, fora do Git) | A senha do banco, gerada por `random_password` |
| `terraform.tfvars` da VPS (mesmo diretório, fora do Git) | `docker_host` (socket local), domínio, e-mail do certificado, memória |

## Pré-requisitos (uma vez)

1. **Docker na VPS:** já instalado na VPS da HostGator; se faltar, `curl -fsSL https://get.docker.com | sh`.
2. **SSH por chave**, com um usuário do grupo `docker` (`sudo usermod -aG docker <usuario>`). Do seu computador,
   `ssh usuario@ip docker info` precisa funcionar sem pedir senha.
3. **Portas 80 e 443 livres e liberadas** no firewall da VPS (`ufw` ou `firewalld`). Nada mais pode estar usando
   essas portas na VPS.
4. **Domínio apontando para a VPS:** um registro DNS `A` com o IP da VPS (por exemplo `api.seudominio.com.br`). O
   Let's Encrypt não emite certificado para IP puro. **Sem domínio**, use o [sslip.io](https://sslip.io): o nome
   `143-95-161-158.sslip.io` resolve para `143.95.161.158`, sem cadastro. É o endereço do deploy atual.
5. **Imagem publicada:** o workflow `.github/workflows/imagem.yml` publica `ghcr.io/c-nnett/agendabeleza-api:<sha>`
   a cada push. Na primeira vez, deixe o pacote público em GitHub → Packages → agendabeleza-api → Package settings.
6. **Terraform 1.9 ou mais novo** no seu computador.

## Deploy automático (GitHub Actions)

O estado e o `terraform.tfvars` ficam na VPS, em `/opt/agendabeleza/terraform`, e o Terraform roda lá, num contêiner
(`hashicorp/terraform`), falando com o Docker pelo socket local. Ninguém precisa do estado na própria máquina.

```text
push na master ──▶ CI ──────────────┐
               └─▶ Imagem (GHCR) ──▶ Deploy: espera o CI passar ─▶ ssh deploy@VPS ─▶ aplicar.sh <sha> ─▶ smoke test
```

- **`.github/workflows/deploy.yml`** dispara quando o workflow `Imagem` termina com sucesso na `master`. Ele espera o
  CI do mesmo commit, envia só os arquivos de configuração (`*.tf`, `Caddyfile.tftpl`, `.terraform.lock.hcl` e
  `aplicar.sh`) e roda `aplicar.sh <sha>` por SSH com o usuário `deploy` (grupo `docker`, chave só do Actions).
  No fim, confere `/actuator/health`.
- **`aplicar.sh`** roda `terraform init` e `apply -auto-approve`. Sem versão, reaplica a que está no ar
  (`terraform output versao_publicada`): serve para mudanças só de infraestrutura.
- **Secrets do environment `production`:** `VPS_HOST`, `VPS_PORTA`, `VPS_USUARIO`, `VPS_CHAVE_SSH` e
  `VPS_KNOWN_HOSTS` (a chave do servidor fixada). **Variável:** `URL_PUBLICA`.
- **Deploy manual:** Actions → Deploy → Run workflow, com o sha ou vazio; ou, na VPS, como `deploy`,
  `/opt/agendabeleza/terraform/aplicar.sh <sha>`.
- **Voltar atrás:** o mesmo Run workflow, com o sha anterior.

## Primeira publicação (sem estado ainda)

Foi como o deploy nasceu em 27/11, da máquina do autor, por SSH. Depois o estado foi copiado para a VPS, e o
`terraform plan` lá deu "No changes". Para recriar do zero em outra VPS, siga os passos abaixo e copie o estado.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # preencha VPS, domínio, e-mail e o sha da imagem
terraform init
terraform plan
terraform apply
terraform output url_swagger
```

O `apply` espera a API ficar saudável (`/actuator/health`). Na primeira subida, o Caddy leva alguns segundos para
obter o certificado.

## Atualizar, voltar atrás e encerrar

- **Nova versão:** automática no push da `master` (seção acima). Só o contêiner da API é recriado, com uma
  interrupção curta. Banco, certificados e dados ficam.
- **Logs:** `ssh usuario@ip docker logs -f agendabeleza-api` (com SSH em outra porta, `ssh -p <porta> ...`).
- **E-mails (Mailpit):** a interface só escuta no `127.0.0.1` da VPS. Rode `ssh -L 8025:127.0.0.1:8025 usuario@ip`
  e abra http://localhost:8025.
- **Encerrar:** na VPS, `terraform destroy` (pelo mesmo contêiner do `aplicar.sh`) remove contêineres, rede e volumes. Isso inclui o banco: faça um backup antes se
  precisar dos dados (`docker exec postgres pg_dump -U agendabeleza agendabeleza > backup.sql`).
