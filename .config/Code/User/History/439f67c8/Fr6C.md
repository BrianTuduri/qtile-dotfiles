# Documentación de Limpieza Automática de Directorios Antiguos

Este documento explica la implementación de una tarea programada para limpiar automáticamente directorios antiguos dentro del directorio `/slave/.tmp_builds/tmp` en sistemas Rocky Linux 9.2. Utilizamos un timer de systemd para ejecutar un script diariamente a las 02:00 AM.

## Estructura

- **Script de Limpieza**: Un script Bash que busca y elimina directorios con más de 12 horas de antigüedad.
- **Servicio de Systemd**: Define cómo se ejecuta el script.
- **Timer de Systemd**: Programa cuándo se ejecuta el servicio.

## Ubicaciones de los Archivos

- **Script de Limpieza**: `/usr/local/bin/clean_old_dirs.sh`
- **Servicio de Systemd**: `/etc/systemd/system/clean_old_dirs.service`
- **Timer de Systemd**: `/etc/systemd/system/clean_old_dirs.timer`

## Detalles de los Archivos

### Script de Limpieza

- **Ruta**: `/usr/local/bin/clean_old_dirs.sh`
- **Propósito**: Busca y elimina directorios con más de 12 horas de antigüedad en `/slave/.tmp_builds/tmp`.
- **Contenido**:
  ```bash
  #!/bin/bash

  # Define el directorio objetivo
  TARGET_DIR="/slave/.tmp_builds/tmp"

  # Encuentra y elimina directorios dentro de TARGET_DIR con más de 12 horas de antigüedad
  find "$TARGET_DIR" -type d -mmin +720 -exec rm -rf {} +
```