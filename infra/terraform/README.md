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
| Estado do Terraform (`terraform.tfstate`, fora do Git) | A senha do banco, gerada por `random_password` |

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

## Publicar

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

- **Nova versão:** faça o push, espere o workflow `Imagem` e rode `terraform apply -var versao_da_imagem=<sha novo>`.
  Só o contêiner da API é recriado, com uma interrupção curta. Banco, certificados e dados ficam.
- **Voltar atrás:** `terraform apply -var versao_da_imagem=<sha anterior>`.
- **Logs:** `ssh usuario@ip docker logs -f agendabeleza-api` (com SSH em outra porta, `ssh -p <porta> ...`).
- **E-mails (Mailpit):** a interface só escuta no `127.0.0.1` da VPS. Rode `ssh -L 8025:127.0.0.1:8025 usuario@ip`
  e abra http://localhost:8025.
- **Encerrar:** `terraform destroy` remove contêineres, rede e volumes. Isso inclui o banco: faça um backup antes se
  precisar dos dados (`docker exec postgres pg_dump -U agendabeleza agendabeleza > backup.sql`).
