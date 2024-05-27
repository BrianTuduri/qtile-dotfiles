### Kafka cluster

- Todos los nodos de un unico cluster deben tener el mismo KAFKA_KRAFT_CLUSTER_ID

### Ports

-> Todos se tienen que ver por el 9092 y 9093

Node1

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
```

Agregar propiedad para que vaya uno a uno

Keepalived: 172.24.21.158

Se tienen que ver todos los hosts en keepalived por el 9345 (all to all)

### Connect

Para lograr que diferentes instancias de Kafka Connect (como QA, DEV y ARGENTINA) se conecten a puertos diferentes (8083, 8084, 8085 respectivamente), se configura y ejecuta cada instancia de Kafka Connect de forma independiente. Es decir cada una con su propio archivo de configuración connect-distributed.properties modificado para especificar el puerto deseado.

Para cada entorno (QA, DEV, ARGENTINA), tendrás un archivo connect-distributed.properties correspondiente.
En cada archivo de configuración tiene su propio listeners con puerto deseado.

-> Para QA: listeners=HTTP://:8083
-> Para DEV: listeners=HTTP://:8084
-> Para ARGENTINA: listeners=HTTP://:8085

Luego se inicia cada instancia de Kafka Connect con su respectivo archivo de configuración modificado.

Ejemplo

```
sh /kafka/kafka/bin/connect-distributed.sh /ruta/a/QA/connect-distributed.properties
```

Aislamiento de Datos: Asegúrate de que las configuraciones de los topics (offset.storage.topic, config.storage.topic, status.storage.topic) sean únicas para cada entorno si deseas aislar completamente los datos y configuraciones entre QA, DEV y ARGENTINA.

Seguridad y Red: Considera aspectos de seguridad y acceso de red al exponer diferentes puertos.

Siguiendo estos pasos, deberías poder configurar y ejecutar múltiples instancias de Kafka Connect en el mismo clúster de Kafka, cada una escuchando en su propio puerto específico. Esto permite la separación y gestión independiente de los conectores en diferentes entornos de desarrollo y pruebas.

## Aislamiento kafka connect

El aislamiento de datos en Kafka Connect se refiere a asegurar que las configuraciones, los estados y los registros de offset de diferentes instancias de Kafka Connect (por ejemplo, para entornos de QA, DEV y ARGENTINA) estén separados entre sí. Esto es crucial cuando se ejecutan múltiples instancias de Kafka Connect que no deben interferir entre ellas, como podría ser el caso de distintos entornos de desarrollo y pruebas.

Para lograr este aislamiento, se debe configurar los siguientes topics para que sean únicos para cada instancia de Kafka Connect:

offset.storage.topic: Este topic almacena los offsets de los registros procesados por los conectores. Asegurar que cada entorno tenga su propio offset.storage.topic previene que las instancias de Connect interfieran entre sí al manejar los offsets de los registros.

config.storage.topic: Este topic guarda las configuraciones de los conectores. Usar un config.storage.topic distinto para cada entorno permite gestionar y aislar las configuraciones de los conectores de manera independiente.

status.storage.topic: Este topic registra el estado de los conectores y sus tareas. Tener un status.storage.topic separado asegura que el seguimiento del estado de cada conector se mantenga aislado entre diferentes entornos de Connect.

Ejemplo de Configuración para Aislamiento
Para los tres entornos mencionados (QA, DEV, ARGENTINA), podrías configurar los topics de la siguiente manera:

QA:

offset.storage.topic=qa-connect-offsets
config.storage.topic=qa-connect-configs
status.storage.topic=qa-connect-status
DEV:

offset.storage.topic=dev-connect-offsets
config.storage.topic=dev-connect-configs
status.storage.topic=dev-connect-status
ARGENTINA:

offset.storage.topic=ARGENTINA-connect-offsets
config.storage.topic=ARGENTINA-connect-configs
status.storage.topic=ARGENTINA-connect-status

Al designar topics específicos para cada entorno en sus respectivos archivos connect-distributed.properties, aseguras que cada instancia de Kafka Connect opere de manera aislada, previniendo interferencias y posibilitando la gestión independiente de los conectores en cada entorno.

## A tener en cuenta

Cuando Kafka-UI se comunica con el cluster de Kafka para preguntar sobre los nodos, el retorna información con el hostname, no la IP. Por ende, la instancia de la UI no sabe que es ese hostname, por esa razon en ./terraform/main.tf se le pasa al values de su Helm Chart el hostAliases:

```terraform-docs
host_aliases = [
    { ip = "172.24.21.29", hostnames = ["srv-desa-int-sp-datatransfer-kafka-master"] },
    { ip = "172.24.21.30", hostnames = ["srv-desa-int-sp-datatransfer-kafka-slave1"] },
    { ip = "172.24.21.31", hostnames = ["srv-desa-int-sp-datatransfer-kafka-slave2"] },
]
```