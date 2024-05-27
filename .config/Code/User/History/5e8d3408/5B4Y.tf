locals { namespace = "kafka-ui-oxxo" }

variable "SPRING_LDAP_USERNAME" {
  type = string
}

variable "SPRING_LDAP_PASSWORD" {
  type = string
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
    "config.yml"             = "${file("./config.yml")}"
    "DYNAMIC_CONFIG_ENABLED" = true
  }
}

resource "kubernetes_secret" "ldap_auth" {
  metadata {
    name      = "ldap-auth-secret"
    namespace = local.namespace
  }

  data = {
    # conexion
    "AUTH_TYPE"                = "LDAP"
    "SPRING_LDAP_URLS"         = "ldap://172.24.18.180:389"
    "SPRING_LDAP_USERNAME"     = var.SPRING_LDAP_USERNAME
    "SPRING_LDAP_PASSWORD"     = var.SPRING_LDAP_PASSWORD
    "SPRING_LDAP_BASE"         = "ou=Uruguay,ou=Geocom,dc=geocom,dc=com,dc=uy"
    "SPRING_LDAP_READ_TIMEOUT" = 5000

  user-filter-search-base: "dc=planetexpress,dc=com"

    # users
    #"SPRING_LDAP_USER_SEARCH_BASE"   = "OU=Uruguay,OU=Geocom"
    "SPRING_LDAP_USER_FILTER_SEARCH_BASE" = "(sAMAccountName={0})"

    # LDAP Authentication
    #"SPRING_SECURITY_LDAP_USERNAME"             = "cn"
    #"SPRING_SECURITY_LDAP_USER_SEARCH_BASE"     = "OU=Uruguay,OU=Geocom"
    #"SPRING_SECURITY_LDAP_GROUP_SEARCH_BASE"    = "OU=Groups"
    #"SPRING_SECURITY_LDAP_GROUP_SEARCH_FILTER"  = "(member={0})"
    #"SPRING_SECURITY_LDAP_GROUP_ROLE_ATTRIBUTE" = "cn"



    # groups
    #"SPRING_LDAP_GROUP_SEARCH_BASE"   = "OU=Groups,DC=geocom,DC=com,DC=uy"

    # logs
    "LOGGING_LEVEL_ROOT" = "debug"
  }
}

module "kafka-ui" {
  depends_on  = [kubernetes_config_map.config-test, kubernetes_secret.ldap_auth]
  source      = "git::https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/terraform_modules/kafka-ui.git"
  namespace   = local.namespace
  ingressHost = "kafkaui-oxxo.geocom.com.uy"
  tlsEnabled  = true
  # qa-oxxo-kafkaui-qa.geocom.com.uy
  # qa-express-kafkaui-qa.geocom.com.uy
  # proyecto-ambiente-servicio.geocoom.com.uy
  existingConfigMapEnv = kubernetes_config_map.config-test.metadata[0].name # envs
  existingSecretName   = kubernetes_secret.ldap_auth.metadata[0].name       # name secret
  yamlApplicationConfigConfigMap = {
    name    = kubernetes_config_map.config-test.metadata[0].name
    keyName = "config.yml"
  }
  host_aliases = [
    { ip = "172.24.26.130", hostnames = ["srv-qa-int-sp-scm-kafka1"] },
    { ip = "172.24.26.131", hostnames = ["srv-qa-int-sp-scm-kafka2"] },
    { ip = "172.24.26.132", hostnames = ["srv-qa-int-sp-scm-kafka3"] },
  ]
}