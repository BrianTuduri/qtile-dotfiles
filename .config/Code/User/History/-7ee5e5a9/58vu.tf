variable "namespace" { type = string }
variable "registry" {
  type = string
  default = "nexus-mirror.geocom.com.uy"
}
variable "ingressHost" {
  type = string
}
variable "existingConfigMapEnv" {
  type = string
}
variable "yamlApplicationConfigConfigMap" {
  type = object({
    name = string
    keyName = string
  })
  #default = [{}]
}

variable "host_aliases" {
  description = "List of host aliases to apply to the pod"
  type = list(object({
    ip        = string
    hostnames = list(string)
  }))
  default = []
}

resource "helm_release" "kafka-ui" {
  name             = "kafka-ui"
  chart            = "kafka-ui"
  version          = "0.7.5"
  namespace        = var.namespace
  create_namespace =  false
  repository       = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  values = [
    yamlencode({
      hostAliases = var.host_aliases
      yamlApplicationConfigConfigMap = var.yamlApplicationConfigConfigMap
      existingConfigMap = var.existingConfigMapEnv
      image = {
        registry = var.registry
      }
      ingress = {
        enabled = true
        host = var.ingressHost
        ingressClassName = "nginx"
      }
    })
  ]
}