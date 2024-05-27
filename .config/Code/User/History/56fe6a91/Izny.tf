variable "namespace" { type = string }
variable "global_registry" { type = string }

# variable "key_location" {
#   type      = string
#   sensitive = true
# }
# variable "crt_location" {
#   type      = string
#   sensitive = true
# }
# resource "kubernetes_secret" "ca_key_pair" {
#   metadata {
#     name      = "cert-manager-private-key"
#     namespace = var.namespace
#   }
#   data = {
#     "tls.crt" = filebase64(var.crt_location) # TODO Treat as sensitive
#     "tls.key" = filebase64(var.key_location)
#   }
# }
resource "kubernetes_manifest" "configure-ingress" {
  manifest = {
    apiVersion = "helm.cattle.io/v1"
    kind       = "HelmChartConfig"
    metadata = {
      name      = "rke2-ingress-nginx"
      namespace = var.namespace
    }
    spec = {
      valuesContent = yamlencode({
        controller = {
          image = { repository = "${var.global_registry}/rancher/nginx-ingress-controller" }
          service = {
            enabled = true
            type    = "LoadBalancer"
          }
          publishService = { enabled = true }
          config = {
            "use-forwarded-headers" = "true"
            "enable-real-ip"        = "true"
            "proxy-body-size"       = "10m"
            "custom-http-errors"    = "404,500,501,502,503"
          }
          extraArgs = {
            "default-ssl-certificate" = "kube-system/geocom-tls"
            #"default-ssl-certificate" = "${kubernetes_secret.ca_key_pair.metadata.0.namespace}/${kubernetes_secret.ca_key_pair.metadata.0.name}"
          }
          allowSnippetAnnotations = true
        }
        defaultBackend = {
          enabled = true
          image = {
            repository = "nexus-mirror.geocom.com.uy/tarampampam/error-pages"
            tag        = "latest"
          }
          extra_envs = [
            {
              name  = "TEMPLATE_NAME"
              value = "l7-dark"
            },
            {
              name  = "SHOW_DETAILS" // Optional: enables the output of additional information on error pages
              value = "true"
            }
          ]
        }
      })
      field_manager = { force_conflicts = true }
    }
  }
}