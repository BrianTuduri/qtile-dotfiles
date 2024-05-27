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
      ha = {
        enabled     = true
        replicas: 3
        apiAddr     = "vault-server.geocom.com.uy"
        clusterAddr = "vault-server.geocom.com.uy"
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
# server.ha.clusterAddr

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