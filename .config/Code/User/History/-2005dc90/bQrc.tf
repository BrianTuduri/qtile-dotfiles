# Create admin policy in the root namespace
resource "vault_policy" "admin_policy" {
  name   = "admins"
  policy = file("policies/admin-policy.hcl")
}

resource "vault_policy" "qa_policy" {
  name   = "qa-policy"
  policy = file("policies/qa-policies.hcl")
}

resource "vault_policy" "prod_policy" {
  name   = "prod-policy"
  policy = file("policies/prod-policies.hcl")
}

