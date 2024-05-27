variable "teams" {
  type    = set(string)
  default = ["Alkosto", "OMS", "Equipo3"]
}

variable "environments" {
  type    = set(string)
  default = ["qa", "prod"]
}

resource "vault_mount" "kv_v2" {
  for_each = { for team in var.teams : team => { for env in var.environments : env => "secrets/${team}/${env}" } }
  path     = each.value
  type     = "kv-v2"
  options  = {
    version = "2"
  }
}

resource "vault_policy" "team_policies" {
  for_each = { for team in var.teams : team => { for env in var.environments : env => "secrets/${team}/${env}" } }

  name   = "${each.key}-${each.value}"
  policy = <<-EOT
    path "${each.value}/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
    path "${each.value}/data/*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
  EOT
}

