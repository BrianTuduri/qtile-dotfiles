variable "teams" {
  type    = set(string)
  default = ["Alkosto", "OMS", "Equipo3"]
}

variable "environments" {
  type    = set(string)
  default = ["qa", "prod"]
}

locals {
  team_envs = [for team in var.teams : [for env in var.environments : "${team}/${env}"]]
}

resource "vault_mount" "kv_v2" {
  for_each = var.teams
  path     = "secrets/${each.value}"
  type     = "kv-v2"
  options = {
    version = "2"
  }
}

resource "vault_kv_secret_backend_v2" "example" {
  for_each = var.environments
  mount                = "${vault_mount.kv_v2.path}/${each.value}"
  max_versions         = 5
  delete_version_after = 12600
  cas_required         = true
}


resource "vault_policy" "team_policies" {
  for_each = toset(flatten(local.team_envs))
  name     = "${each.value}-policy"
  policy   = <<-EOT
    path "secrets/${each.value}/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
    path "secrets/${each.value}/data/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
  EOT
}
