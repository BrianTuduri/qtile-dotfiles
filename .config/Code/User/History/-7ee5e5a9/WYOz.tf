
variable namespace {
  type        = string
}

resource "helm_release" "kafka-ui" {
  name       = "kafka-ui"
  repository = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  chart      = "nfs-unit"
  version    = "0.0.3"
  namespace  = var.namespace
  values = [
    file("./values.yml"),
  ]
}