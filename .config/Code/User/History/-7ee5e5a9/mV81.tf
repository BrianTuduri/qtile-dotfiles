
variable namespace {
  type        = string
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
  set {
    name = existingConfigMapConfig
    value = var.existingConfigMapConfig
  }
}