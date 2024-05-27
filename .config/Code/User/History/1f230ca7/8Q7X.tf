
locals {
  namespace  = "kafka-ui-test"
}


module kafka-ui {
  depends_on = [kubernetes_config_map.config-test]
  source = "../kafka-ui/"
  namespace  = local.namespace
  existingConfigMapConfig = kubernetes_config_map.config-test.metadata[0].name # Configuracion
  existingConfigMapConfigKey = "config-test.yml" # Configuracion
  existingConfigMapEnv = kubernetes_config_map.config-test.metadata[0].name # envs
  host_aliases = [
    {
      ip = "172.24.26.130"
      hostnames = ["node1"]
    },
    {
      ip = "172.24.26.131"
      hostnames = ["node2"]
    },
    {
      ip = "172.24.26.132"
      hostnames = ["node3"]
    }
  ]
}

resource "kubernetes_config_map" "config-test" {
  
  metadata {
    name = "config-test"
    namespace = local.namespace
  }

  data = {
    "config-test.yml" = "${file("../kafka-ui/config-test.yml")}"
    "DYNAMIC_CONFIG_ENABLED" = true
  }
}

