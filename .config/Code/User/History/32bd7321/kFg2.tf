variable "kube_config_path" {
  type        = string
  description = "Path to the Kubernetes config file."
  default     = "~/.kube/qa.yml"
}

variable "namespace" {
  description = "Kubernetes namespace"
  default     = "vault"
}