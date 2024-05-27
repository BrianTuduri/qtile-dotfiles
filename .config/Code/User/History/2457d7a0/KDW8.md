### add topic

agrega los topicos al cluster

### add connect 

Inicializa la conexion segun el ambiente que sea

QA -> 8083
DEV -> 8084
ARG -> 8085
BETA -> 8086

### add connectors

Segun el ambiente, aprovisiona los connectors a un connect.

QA -> 8083
DEV -> 8084
ARG -> 8085
BETA -> 8086

Esto lo hace mediante API REST con el JSON de el connector

### delete connectors 

Elimina los connectors que se le especifique