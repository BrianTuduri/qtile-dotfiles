resource "vault_auth_backend" "ldap" {
  type = "ldap"
  description = "LDAP Authentication"
}

resource "vault_ldap_auth_backend_config" "ldap" {
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

resource "vault_policy" "user" {
  name   = "user-policy"
  policy = <<EOT
path "secret/data/users/{{identity.entity.id}}/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "secret/metadata/users/{{identity.entity.id}}/*" {
  capabilities = ["list"]
}
EOT
}

resource "vault_ldap_auth_backend_group" "developers" {
  backend   = vault_auth_backend.ldap.path
  groupname = "developers"
  policies  = [vault_policy.user.name]
}