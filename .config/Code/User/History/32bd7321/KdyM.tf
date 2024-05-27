variable "kube_config_path" {
  type        = string
  description = "Path to the Kubernetes config file."
  default     = "~/.kube/qa.yml"
}

variable "namespace" {
  type = string
  description = "Kubernetes namespace"
  default     = "vault"
}