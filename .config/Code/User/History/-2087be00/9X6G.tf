provider "kubernetes" {}
//provider "rancher2" {}
provider "helm" {
  kubernetes {}
}
terraform {
  required_providers {
    helm       = { source = "hashicorp/helm" }
    kubernetes = { source = "hashicorp/kubernetes" }
    //rancher2   = { source = "rancher/rancher2" }
  }
}
provider "vault" {
  address = "https://vault-server.geocom.com.uy"
  token   = "hvs.0n8IkqQuXwjfj5hsl29v1Z0L"
}