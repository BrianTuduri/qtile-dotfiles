variable "ldapUser" { 
  type = string
  sensitive = true 
}
variable "ldapPass" { 
  type = string
  sensitive = true
}

resource "vault_ldap_auth_backend" "ldap" {
  path        = "ldap"
  url         = "ldap://172.24.18.180:389"
  userdn      = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
  userattr    = "sAMAccountName"
  userfilter  = "(sAMAccountName={{.Username}})"
  discoverdn  = false
  groupdn     = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
  groupfilter = "(&(objectClass=group)(member:1.2.840.113556.1.4.1941:={{.UserDN}}))"
  binddn      = var.ldapUser
  bindpass    = var.ldapPass
}

resource "vault_ldap_auth_backend_group" "group" {
  groupname = "geoscm"
  policies  = ["admins"]
  backend   = vault_ldap_auth_backend.ldap.path
}