variable "group_policies" {
  type = map(list(string))
  default = {
    "geoscm" = ["admins"],
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
