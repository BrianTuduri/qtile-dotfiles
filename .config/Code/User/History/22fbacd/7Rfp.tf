terraform {
  required_version = ">= 0.14"

  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = ">= 3.6"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 1.22"
    }
  }
}
