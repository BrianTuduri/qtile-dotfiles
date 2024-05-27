variable "namespace" { type = string }
variable "registry" {
  type = string
  default = "nexus-mirror.geocom.com.uy"
}
variable "ingressClassName" {
  type = string
  default = "nginx"
}
variable "ingressHost" {
  type = string
}
variable "existingConfigMapEnv" {
  type = string
}
variable "existingSecretName" {
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
