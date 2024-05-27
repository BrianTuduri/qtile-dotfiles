terraform {
  required_providers {
    rancher2 = { source = "rancher/rancher2" }
  }
  backend "kubernetes" {
    secret_suffix = "geoswitch-test"
  }
}
provider "kubernetes" {}
provider "rancher2" {}
provider "helm" {
  kubernetes {}
}

module "central_namespace" {
  source                = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/rancher_project_namespace"
  create_project        = true
  cluster_id            = var.cluster_id
  project_name          = "Geoswitch-Test"
  namespace_name        = "geoswitch-test"
  notifications_enabled = false
}
variable "cluster_id" { default = "c-m-2jpv2rpb" }
locals { ns = module.central_namespace.namespace }