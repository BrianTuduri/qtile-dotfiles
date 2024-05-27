resource "vault_auth_backend" "ldap" {
  type = "ldap"
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
  binddn      = "gitlabsp@geocom.com.uy"
  bindpass    = "3[6}L'&z^B%sg?&#)W"
}

resource "vault_ldap_auth_backend_user" "user" {
  backend  = vault_ldap_auth_backend.ldap.path
  username = "test-user"
  policies = ["dba", "sysops"]
}