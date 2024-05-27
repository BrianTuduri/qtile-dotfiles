# Generate namespace


variable "cluster_id" { type = string }
variable "project_name" { type = string }
variable "namespace_name" { type = string }
variable "rancher_service_account" {}
variable "cluster_name" { type = string }
variable "metallb_default_ip" { type = string }
variable "global_registry" { type = string }

variable "values" {
  type = list(any)
  description = "List of YAML-encoded configurations for components"
}

variable "ldapUser" {
  type      = string
  sensitive = true
}
variable "ldapPass" {
  type      = string
  sensitive = true
}

locals { 
  vault_config = <<EOF
  ui = true
  listener "tcp" {
    tls_disable = 1
    address = "[::]:8200"
    cluster_address = "[::]:8201"
  }
  storage "file" {
    path = "/vault/data"
  }
  log_level = "error"
EOF
}

locals { cluster_registry = "nexus-mirror.geocom.com.uy" }

module "secrets_namespace" {
  source                = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/rancher_project_namespace"
  create_project        = false
  cluster_id            = var.cluster_id
  project_name          = var.project_name
  namespace_name        = var.namespace_name
  notifications_enabled = false
}

resource "helm_release" "vault" {
  namespace  = module.secrets_namespace.namespace
  name       = "cluster-vault"
  chart      = "vault"
  version    = "0.28.0"
  repository = "https://helm.releases.hashicorp.com"
  values = var.values
  wait    = true
  timeout = 600
}