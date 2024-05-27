provider "helm" {
  kubernetes {
    config_path = "${var.kube_config_path}"
  }
}

provider "kubernetes" {
  config_path = "${var.kube_config_path}"
}

resource "helm_release" "vault" {
  name       = "vault"
  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"
  version    = "0.27.0"
  namespace  = "${var.namespace}"

  values = [file("${path.module}/../helm/vault/values.yml")]
}
