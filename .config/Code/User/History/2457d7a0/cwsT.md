### add topic

agrega los topicos al cluster

### add connect 

Inicializa la conexion segun el ambiente que sea

QA -> 8083
DEV -> 8084
ARGENTINA -> 8085
BETA-ORIG -> 8086
CHILE -> 8087

### add connectors

Segun el ambiente, aprovisiona los connectors a un connect.

QA -> 8083
DEV -> 8084
ARGENTINA -> 8085
BETA-ORIG -> 8086
CHILE -> 8087

Esto lo hace mediante API REST con el JSON de el connector

### delete connectors 

Elimina los connectors que se le especifique