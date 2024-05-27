# Enable K/V v2 secrets engine
resource "vault_mount" "kvv2" {
  path        = "kvv2"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 secret engine mount"
}

# Enable K/V v2 secrets engine for prod
resource "vault_mount" "kvv2-prod" {
  path        = "kvv2/prod"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 for Production"
}

# Enable K/V v2 secrets engine for qa
resource "vault_mount" "kvv2-prod" {
  path        = "kvv2/qa"
  type        = "kv"
  options     = { version = "2" }
  description = "KV Version 2 for QA"
}