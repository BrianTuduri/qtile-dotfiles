terraform {
  required_providers {
    helm = {
      source = "hashicorp/helm"
    }
    kubernetes = {
      source = "hashicorp/kubernetes"
    }
  }
}

provider "helm" {
  kubernetes {
    config_path = "../.kube/config"
  }
}

provider "kubernetes" {
  config_path = "../.kube/config"
}

resource "helm_release" "vault" {
  name       = "vault"
  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version    = "0.13.0"  # Asegúrate de verificar la última versión disponible.

  values = [file("${path.module}/../helm/values.yml")]
}
