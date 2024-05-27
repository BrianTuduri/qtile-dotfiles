
def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
    // Extraer el espacio de nombres, el repositorio y la imagen con su etiqueta de la URL base
    // El patrón asume que la URL sigue el formato: <dominio>:<puerto>/<espacio_de_nombres>/<repositorio>/<imagen>:<etiqueta>
    def pattern = /^(.*:\/\/)?([^\/]+)\/(.+):(.+)$/
    def matcher = (baseRegistryUrl =~ pattern)
    if (!matcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado.")
        return
    }
    def registryRepository = matcher.group(3) // uy-com-geocom-geosalud/geosalud-registry/pentaho-server
    def imageVersion = matcher.group(4) // pentaho-server-ce-9.3.0.0-428

    // Construir y retornar la nueva URL utilizando la nueva base del registro
    return "${newRegistryUrl}/${registryRepository}:${imageVersion}"
}

// Prueba de la función
def baseRegistryUrl = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrl = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"

println(newBaseUrl(baseRegistryUrl, newRegistryUrl))
