variable "kube_config_path" {
  type        = string
  description = "Path to the Kubernetes config file."
  default     = "~/.kube/config"
}

variable "namespace" {
  description = "Kubernetes namespace"
  default     = "vault"
}
