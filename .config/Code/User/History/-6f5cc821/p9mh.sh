#!/bin/bash

# Iniciar el daemon de Docker en modo sin privilegios.
dockerd-rootless.sh &

# Ejecutar el comando por defecto de Jenkins Agent (o cualquier otro comando que necesites).
exec "$@"