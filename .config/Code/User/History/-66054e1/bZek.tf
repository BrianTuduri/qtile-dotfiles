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
  path     = "secrets/${each.key}"
  type     = "kv-v2"
  options = {
    version = "2"
  }
}

resource "vault_kv_secret_v2" "kvv2-qa" {
  depends_on = [vault_mount.kv_v2]
  for_each   = var.teams
  mount      = "${vault_mount.kv_v2[each.key].path}/qa"
  name       = "secret-${vault_mount.kv_v2[each.key].path}-qa"
  data_json = jsonencode({
    "example" = "example"
  })
}

resource "vault_kv_secret_v2" "kvv2-prod" {
  depends_on = [vault_mount.kv_v2]
  for_each   = var.teams
  mount      = "${vault_mount.kv_v2[each.key].path}/prod"
  name       = "secret-${vault_mount.kv_v2[each.key].path}-prod"
  data_json = jsonencode({
    "example" = "example"
  })
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
