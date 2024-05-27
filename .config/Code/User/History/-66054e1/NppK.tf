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

resource "vault_generic_secret" "qa-secrets" {
  depends_on = [vault_mount.kv_v2]
  for_each   = var.teams
  path       = "${vault_mount.kv_v2[each.key].path}/qa/do-not-use-me"
  data_json = jsonencode({
    placeholder = "This is a placeholder to create the QA folder."
  })
  //disable_read = true
}

resource "vault_generic_secret" "prod-secrets" {
  depends_on = [vault_mount.kv_v2]
  for_each   = var.teams
  path       = "${vault_mount.kv_v2[each.key].path}/prod/do-not-use-me/"
  data_json = jsonencode({
    placeholder = "This is a placeholder to create the QA folder."
  })
  //disable_read = true
}

resource "vault_policy" "team_policies" {
  for_each = var.teams
  name     = "${each.value}-policy-qa"
  policy   = <<-EOT
    path "secrets/${each.value}/data/qa/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
    path "secrets/${each.value}/metadata/qa/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
  EOT
}