variable "metallb_default_ip" { type = string }
# module "networking" {
#   source           = "${local.modules_path}/networking"
#   namespace        = "kube-system"
#   metallb_ip_range = var.metallb_default_ip
#   global_registry  = var.global_registry
# }
# 
module "ingress" {
  source          = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/ingress.git"
  namespace       = "kube-system"
  global_registry = var.global_registry
  # crt_location = ""
  # key_location = ""
}
module "ingress-rules" {
  for_each       = fileset("${path.module}", "alerts/storage/*.yml")
  source         = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/prometheus-rule"
  namespace      = module.monitoring_namespace.namespace
  rule_file_path = abspath("./${each.key}")
}