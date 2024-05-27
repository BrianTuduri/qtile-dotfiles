variable "environments" {
  type    = set(string)
  default = ["qa", "prod"]
}

resource "vault_mount" "kv_v2" {
  for_each = var.environments
  path     = "secrets-${each.value}"
  type     = "kv-v2"
  options = {
    version = "2"
  }
}
