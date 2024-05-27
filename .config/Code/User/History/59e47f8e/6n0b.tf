locals {
    values = [yamlencode({
        global = { namespace = module.secrets_namespace.namespace }
        ui     = { enabled = true }
        server = {
            image = { repository = "${local.cluster_registry}/hashicorp/vault" }
            dataStorage = {
                enabled = true,
                size    = "10Gi",
                mountPath : "/vault"
            }
            ingress = {
                enabled = true
                hosts   = [{ host = "vault.geocom.com.uy", paths = ["/"] }]
                tls     = [{ host = "vault.geocom.com.uy" }]
            }
            standalone = {
                config = local.vault_config
            }
            extraEnvironmentVars = {
                VAULT_ADDR = "https://vault.geocom.com.uy"
            }
            persistentVolumeClaimRetentionPolicy = {
                whenDeleted = "Retain"
                whenScaled  = "Retain"
            }
        }
        injector = {
            image      = { repository = "${local.cluster_registry}/hashicorp/vault-k8s" }
            agentImage = { repository = "${local.cluster_registry}/hashicorp/vault" }
        }
    })]
}


module "vault" {
    source          = "git::https://gitlab.geocom.com.uy/scm/DevOPS/terraform-modules/ingress.git"
    namespace       = "kube-system"
    global_registry = var.global_registry
    cluster_id            = "c-m-2jpv2rpb" //data.rancher2_cluster.imported_cluster.id
    project_name          = "Vault"
    namespace_name        = "vault"
    values = locals.values
}