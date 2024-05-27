# Generate namespace
module "secrets_namespace" {
  source                = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/rancher_project_namespace"
  create_project        = false
  cluster_id            = "c-m-2jpv2rpb" //data.rancher2_cluster.imported_cluster.id
  project_name          = "Vault"
  namespace_name        = "vault"
  notifications_enabled = false
}

variable "rancher_service_account" {}
variable "cluster_name" { type = string }
variable "metallb_default_ip" { type = string }
//variable "metallb_ip_range" { type = string }
//variable "cluster_id" { type = string }
variable "global_registry" { type = string }
//variable "service_account_name" { type = string }

# data "rancher2_cluster" "imported_cluster" {
#   name = var.cluster_name
# }

locals { cluster_registry = "nexus-mirror.geocom.com.uy" }

resource "helm_release" "vault" {
  namespace  = module.secrets_namespace.namespace
  name       = "cluster-vault"
  chart      = "vault"
  version    = "0.28.0"
  repository = "https://helm.releases.hashicorp.com"
  values = [yamlencode({
    global = { namespace = module.secrets_namespace.namespace }
    ui     = { enabled = true }
    server = {
      image       = { repository = "${local.cluster_registry}/hashicorp/vault" }
      dataStorage = { enabled = true, size = "5Gi" }
      ingress = {
        enabled = true
        hosts   = [{ host = "vault-server.geocom.com.uy", paths = ["/"] }]
        tls     = [{ host = "vault-server.geocom.com.uy" }]
      }
    }
    injector = {
      image      = { repository = "${local.cluster_registry}/hashicorp/vault-k8s" }
      agentImage = { repository = "${local.cluster_registry}/hashicorp/vault" }
    }
  })]
  wait    = true
  timeout = 600
}

resource "vault_ldap_auth_backend" "ldap" {
  path       = "ldap"
  url        = "ldap://172.24.18.180:389"
  binddn     = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
  bindpass   = "3[6}L'&z^B%sg?&#)W"
  userdn     = "gitlabsp@geocom.com.uy"
  userattr   = "sAMAccountName"
  discoverdn = false
  userfilter  = "(sAMAccountName={0})"
  groupdn     = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
  groupfilter = "(objectClass=group)"
}

/* 
"AUTH_TYPE"                             = "LDAP"
"SPRING_LDAP_URLS"                      = "ldap://172.24.18.180:389"
"SPRING_LDAP_BASE"                      = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
"SPRING_LDAP_ADMIN_USER"                = var.SPRING_LDAP_USERNAME
"SPRING_LDAP_ADMIN_PASSWORD"            = var.SPRING_LDAP_PASSWORD
"SPRING_LDAP_USER_FILTER_SEARCH_BASE"   = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
"SPRING_LDAP_USER_FILTER_SEARCH_FILTER" = "(sAMAccountName={0})"
"SPRING_LDAP_GROUP_FILTER_SEARCH_BASE"  = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
"SPRING_LDAP_GROUP_SEARCH_FILTER"       = "(objectClass=group)"
*/