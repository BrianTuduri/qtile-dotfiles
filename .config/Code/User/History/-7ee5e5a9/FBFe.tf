
variable namespace {
  type        = string
}

resource "helm_release" "kafka-ui" {
  name       = "kafka-ui"
  repository = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  chart      = "kafka-ui"
  version    = "0.7.5"
  namespace  = var.namespace
  values = [
    file("./values.yml"),
  ]
}