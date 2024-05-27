
module kafka-ui {
  source = "../kafka-ui/"
  namespace  = "kafka-ui-test"
  existingConfigMapConfig = kubernetes_config_map.config-test.name # Configuracion
  existingConfigMapConfigKey = kubernetes_config_map.config-test.data["config-test.yml"].name # Configuracion
  existingConfigMapEnv = kubernetes_config_map.config-test.name # Configuracion 
}

resource "kubernetes_config_map" "config-test" {
  
  metadata {
    name = "config-test"
    namespace = var.namespace
  }

  data = {
    "config-test.yml" = "${file("${path.module}/config-test.yml")}"
    "DYNAMIC_CONFIG_ENABLED" = true
  }
}
