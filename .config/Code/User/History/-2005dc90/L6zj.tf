# Create admin policy in the root namespace
resource "vault_policy" "admin_policy" {
  name   = "admins"
  policy = file("policies/admin-policy.hcl")
}

# Policies for prod environment
resource "vault_policy" "prod-policy" {
  name   = "prod-policy"
  policy = <<-EOT
  path "kv-v2/prod/*" {
    capabilities = ["create", "read", "update", "delete", "list"]
  }
  EOT
}

# Policies for QA environment
resource "vault_policy" "qa-policy" {
  name   = "qa-policy"
  policy = <<-EOT
  path "kv-v2/qa/*" {
    capabilities = ["create", "read", "update", "delete", "list"]
  }
  EOT
}
