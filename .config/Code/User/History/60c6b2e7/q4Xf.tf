resource "helm_release" "kafka-ui" {
  name             = "kafka-ui"
  chart            = "kafka-ui"
  version          = "0.7.5"
  namespace        = var.namespace
  create_namespace = false
  repository       = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  values = [
    yamlencode({
      hostAliases = var.host_aliases
      yamlApplicationConfigConfigMap = var.yamlApplicationConfigConfigMap
      existingConfigMap = var.existingConfigMapEnv
      #existingSecret = var.existingSecretName
      image = {
        registry = var.registry
      }
      ingress = {
        enabled = true
        host = var.ingressHost
        ingressClassName = var.ingressClassName
      }
      envFrom = [
        {
          secretRef = {
            name = "ldap-auth-secret"
          }
        }
      ]
    })
  ]
}
