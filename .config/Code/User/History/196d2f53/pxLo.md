# Documentación del Cluster Kafka

Este documento contiene las configuraciones esenciales y guías para el manejo adecuado del cluster Kafka, incluyendo detalles sobre puertos, Kafka Connect y el aislamiento de datos en Kafka Connect.

## Configuración General del Cluster

### Identificador del Cluster

- Todos los nodos de un único cluster deben tener el mismo `KAFKA_KRAFT_CLUSTER_ID`. Esto se puede chequear con:

```bash
cat /kafka/var/lib/kafka/meta.properties
```

### Puertos

- Todos los nodos deben ser accesibles a través de los puertos `9092` y `9093`.

#### Configuración de Ejemplo para un Nodo

```properties
# Ansible managed
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@172.24.21.29:9093,2@172.24.21.30:9093,3@172.24.21.31:9093
listeners=BROKER://:9092,CONTROLLER://:9093
inter.broker.listener.name=BROKER
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,BROKER:PLAINTEXT
log.dirs=/kafka/var/lib/kafka

Keepalived

    IP de Keepalived: 172.24.21.158.
    Todos los hosts deben ser visibles en Keepalived a través del puerto 9345 (all to all).

Configuración de Kafka Connect

Para conectar diferentes instancias de Kafka Connect (como QA, DEV y ARGENTINA) a puertos distintos (8083, 8084, 8085 respectivamente), es necesario configurar y ejecutar cada instancia de Kafka Connect de manera independiente, con su propio archivo connect-distributed.properties modificado para especificar el puerto deseado.
Ejemplo de Configuración

    Para QA: listeners=HTTP://:8083
    Para DEV: listeners=HTTP://:8084
    Para ARGENTINA: listeners=HTTP://:8085

Iniciar Kafka Connect

sh

sh /kafka/kafka/bin/connect-distributed.sh /ruta/a/QA/connect-distributed.properties

Aislamiento de Datos en Kafka Connect

Es crucial asegurar que las configuraciones de los topics (offset.storage.topic, config.storage.topic, status.storage.topic) sean únicas para cada entorno (QA, DEV, ARGENTINA) para lograr un completo aislamiento de datos y configuraciones.
Ejemplo de Configuración para Aislamiento

    QA:
        offset.storage.topic=qa-connect-offsets
        config.storage.topic=qa-connect-configs
        status.storage.topic=qa-connect-status

    DEV:
        offset.storage.topic=dev-connect-offsets
        config.storage.topic=dev-connect-configs
        status.storage.topic=dev-connect-status

    ARGENTINA:
        offset.storage.topic=argentina-connect-offsets
        config.storage.topic=argentina-connect-configs
        status.storage.topic=argentina-connect-status

Consideraciones Adicionales
Kafka-UI y la Comunicación con el Cluster

Cuando Kafka-UI se comunica con el cluster de Kafka para preguntar sobre los nodos, Kafka retorna información con el hostname, no la IP. Para resolver este problema y permitir que la instancia de la UI identifique correctamente a los nodos, se utilizan hostAliases en la configuración de Terraform:

hcl

host_aliases = [
    { ip = "172.24.21.29", hostnames = ["srv-desa-int-sp-datatransfer-kafka-master"] },
    { ip = "172.24.21.30", hostnames = ["srv-desa-int-sp-datatransfer-kafka-slave1"] },
    { ip = "172.24.21.31", hostnames = ["srv-desa-int-sp-datatransfer-kafka-slave2"] },
]

Siguiendo estas configuraciones y consideraciones, podrás gestionar y mantener el cluster Kafka y sus instancias de Kafka Connect de manera eficiente y segura.

Asegúrate de revisar y adaptar cualquier sección según los detalles específicos de tu proyecto o entorno.

css


Este formato debería ser adecuado para un archivo README en repositorios de GitHub o similares, proporcionando una visión clara y estructurada del contenido y las instrucciones necesarias.