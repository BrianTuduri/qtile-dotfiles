locals { namespace = "kafka-ui-oxxo" }

variable SPRING_LDAP_USERNAME {
  type        = string
}

variable SPRING_LDAP_PASSWORD {
  type        = string
}


resource "kubernetes_namespace" "namespace" {
  count = 0
  metadata {
    name = local.namespace
  }
}

resource "kubernetes_config_map" "config-test" {
  metadata {
    name      = "config-test"
    namespace = local.namespace
  }
  data = {
    "config.yml"        = "${file("./config.yml")}"
    "DYNAMIC_CONFIG_ENABLED" = true
  }
}

resource "kubernetes_secret" "ldap_auth" {
  metadata {
    name = "ldap-auth-secret"
    namespace = local.namespace
  }

  data = {
    "AUTH_TYPE"                          = "LDAP"
    "SPRING_LDAP_URLS"                   = "ldap://172.24.18.180:389"
    "SPRING_LDAP_USERNAME" = var.SPRING_LDAP_USERNAME
    "SPRING_LDAP_PASSWORD" = var.SPRING_LDAP_PASSWORD
    "SPRING_LDAP_BASE"                   = "OU=Uruguay,OU=Geocom,DC=geocom,DC=com,DC=uy"
    "SPRING_LDAP_USER_DN_PATTERN"        = "sAMAccountName={0}"
    #"SPRING_LDAP_USER_FILTER_SEARCH_BASE"= "OU=Uruguay,OU=Geocom,DC=geocom,DC=com,DC=uy"
    "SPRING_LOGGIN_LEVEL_ROOT" = "DEBUG"
    #"SPRING_LDAP_USER_FILTER_SEARCH_FILTER"= ""
    #"SPRING_LDAP_GROUP_FILTER_SEARCH_BASE"= "OU=Uruguay,OU=Geocom,DC=geocom,DC=com,DC=uy"gitlabsp@geocom.com.uy
    # (&(sAMAccountName={0})q(objectClass=person)(|(memberof=CN=OU=Uruguay,OU=Geocom,DC=geocom,DC=com,DC=uy)))
  }
}

module "kafka-ui" {
  depends_on           = [kubernetes_config_map.config-test, kubernetes_secret.ldap_auth]
  source                = "git::https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/terraform_modules/kafka-ui.git"
  namespace            = local.namespace
  ingressHost          = "kafkaui-oxxo.geocom.com.uy"
  # qa-oxxo-kafkaui-qa.geocom.com.uy
  # qa-express-kafkaui-qa.geocom.com.uy
  # proyecto-ambiente-servicio.geocoom.com.uy
  existingConfigMapEnv = kubernetes_config_map.config-test.metadata[0].name # envs
  existingSecretName = kubernetes_secret.ldap_auth.metadata[0].name # name secret
  yamlApplicationConfigConfigMap = {
    name = kubernetes_config_map.config-test.metadata[0].name
    keyName  = "config.yml"
  }
  host_aliases = [
    { ip = "172.24.26.130", hostnames = ["srv-qa-int-sp-scm-kafka1"] },
    { ip = "172.24.26.131", hostnames = ["srv-qa-int-sp-scm-kafka2"] },
    { ip = "172.24.26.132", hostnames = ["srv-qa-int-sp-scm-kafka3"] },
  ]
}