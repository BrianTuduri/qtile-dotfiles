# Enable K/V v2 secrets engine
resource "vault_mount" "kvv2" {
  path        = "kvv2"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 secret engine mount"
}

# Enable K/V v2 secrets engine for prod
resource "vault_mount" "kvv2-prod" {
  path        = "secret-prod"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 for Production"
}

resource "vault_mount" "kvv2-qa" {
  path        = "secret-qa"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 for QA"
}