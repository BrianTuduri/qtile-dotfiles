provider "kubernetes" {}
provider "helm" {
  kubernetes {}
}
terraform {
  required_providers {
    helm       = { source = "hashicorp/helm" }
    kubernetes = { source = "hashicorp/kubernetes" }
  }
}
# provider "vault" {
#   address = "https://vault-server.geocom.com.uy"
#   token   = "hvs.C6CnsAlT1Y4vsqGsKS3C0egB"
# }