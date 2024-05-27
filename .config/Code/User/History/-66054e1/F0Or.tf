variable "environments" {
  type = set(string)
  default = ["qa", "prod"]
}

resource "vault_namespace" "env" {
  for_each = var.environments
  path     = each.value
}

resource "vault_mount" "kv_v2" {
  for_each  = var.environments
  namespace = "${vault_namespace.env[each.value].path}"
  path      = "secrets-${vault_namespace.env[each.value].path}"
  type      = "kv-v2"
  options   = {
    version = "2"
  }
}
