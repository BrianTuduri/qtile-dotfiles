
variable namespace {
  type        = string
}

resource "kubernetes_config_map" "config-test" {
  metadata {
    name = "config-test"
  }

  data = {
    "config-test.yml" = "${file("${path.module}/values.yml")}"
  }
}

resource "helm_release" "kafka-ui" {
  name       = "kafka-ui"
  repository = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  chart      = "kafka-ui"
  version    = "0.7.5"
  namespace  = var.namespace
  create_namespace = true
  values = [
    file("${path.module}/values.yml"),
  ]
}