
locals {
  namespace  = "kafka-ui-test"
}


module kafka-ui {
  depends_on = [kubernetes_config_map.config-test, kubernetes_config_map.custom_hosts]
  source = "../kafka-ui/"
  namespace  = local.namespace
  existingConfigMapConfig = kubernetes_config_map.config-test.metadata[0].name # Configuracion
  existingConfigMapConfigKey = "config-test.yml" # Configuracion
  existingConfigMapEnv = kubernetes_config_map.config-test.metadata[0].name # Configuracion 
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

resource "kubernetes_config_map" "custom_hosts" {
  metadata {
    name = "custom-hosts"
  }

  data = {
    "custom-hosts" = <<EOF
172.24.26.130 node1
172.24.26.131 node2
172.24.26.132 node3
EOF
  }
}

