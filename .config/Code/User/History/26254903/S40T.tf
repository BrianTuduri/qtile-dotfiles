provider "kubernetes" {}
provider "rancher2" {}
provider "helm" {
  kubernetes {}
}
terraform {
  required_providers {
    helm       = { source = "hashicorp/helm" }
    kubernetes = { source = "hashicorp/kubernetes" }
    rancher2   = { source = "rancher/rancher2" }
  }
}