# Enable K/V v2 secrets engine for prod and qa
resource "vault_mount" "kv-v2-prod" {
  path        = "kv-v2/prod"
  type        = "kv-v2"
  description = "KV Version 2 for Production"
}

resource "vault_mount" "kv-v2-qa" {
  path        = "kv-v2/qa"
  type        = "kv-v2"
  description = "KV Version 2 for QA"
}