resource "helm_release" "volumenes" {
  for_each   = local.volumesMap
  name       = each.key
  repository = "https://nexus.geocom.com.uy/repository/helm-hosted/"
  chart      = "nfs-unit"
  version    = "0.0.3"
  namespace  = local.namespace
  set {
    name  = "nameOverride"
    value = each.value
  }
  set {
    name  = "endpoints"
    value = "glusterfs-cluster"
  }
  set {
    name  = "IP"
    value = "172.23.206.40"
  }
}