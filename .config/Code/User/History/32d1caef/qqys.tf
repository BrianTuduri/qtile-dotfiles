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
    config_path = "${kube_config_path}"
  }
}

provider "kubernetes" {
  config_path = "${kube_config_path}"
}

resource "helm_release" "vault" {
  name       = "vault"
  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version    = "0.27.0"

  values = [file("${path.module}/../helm/hasicorp-vault/values.yml")]
}
