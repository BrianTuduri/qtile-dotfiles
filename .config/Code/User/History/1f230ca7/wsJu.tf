
locals {
  namespace  = "kafka-ui-test"
}


module kafka-ui {
  depends_on = [kubernetes_config_map.config-test]
  source = "../kafka-ui/"
  namespace  = local.namespace
  existingConfigMapConfig = kubernetes_config_map.config-test.name # Configuracion
  existingConfigMapConfigKey = kubernetes_config_map.config-test.data["config-test.yml"].name # Configuracion
  existingConfigMapEnv = kubernetes_config_map.config-test.name # Configuracion 
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
