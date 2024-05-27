variable "group_policies" {
  type = map(list(string))
  default = {
    "geoscm"            = ["admins"],
    "Soporte GEOSwitch" = ["geoswitch-policy-qa"]
  }
}
variable "user_policies" {
  type = map(list(string))
  default = {
    "svc_gitlab_admin" = ["alkosto-policy-qa"],
  }
}

resource "vault_ldap_auth_backend_group" "group" {
  for_each = var.group_policies

  groupname = each.key
  policies  = each.value
  backend   = vault_ldap_auth_backend.ldap.path
}

resource "vault_ldap_auth_backend_user" "user" {
  for_each = var.user_policies

  username = each.key
  policies = each.value
  backend  = vault_ldap_auth_backend.ldap.path
}

resource "vault_approle_auth_backend_role" "approle_auth_backend_role_teams" {
  depends_on = [vault_auth_backend.approle]
  for_each   = var.user_policies

  backend        = vault_auth_backend.approle.path
  role_name      = each.key
  token_policies = each.value
}

resource "vault_approle_auth_backend_role_secret_id" "id" {
  depends_on = [vault_approle_auth_backend_role.approle_auth_backend_role_teams]
  for_each   = var.user_policies

  backend   = vault_auth_backend.approle.path
  role_name = each.key

  metadata = jsonencode({
    "Team"   = each.key,
    "Policy" = join(",", each.value)
  })
}

output "role_secret_id_accessor" {
  value     = { for k, v in vault_approle_auth_backend_role_secret_id.id : k => v.accessor }
  sensitive = true
}

output "role_secret_id_wrapping_accessor" {
  value     = { for k, v in vault_approle_auth_backend_role_secret_id.id : k => v.wrapping_accessor }
  sensitive = true
}

output "role_secret_id_wrapping_token" {
  value     = { for k, v in vault_approle_auth_backend_role_secret_id.id : k => v.wrapping_token }
  sensitive = true
}
