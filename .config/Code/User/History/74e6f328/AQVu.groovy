def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
    // Actualizar el patrón para manejar URLs con y sin puerto para la baseRegistryUrl
    def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
    def baseMatcher = (baseRegistryUrl =~ basePattern)
    if (!baseMatcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado.")
        return null
    }
    def domain = baseMatcher.group(2) // Dominio sin puerto de la URL base
    def port = baseMatcher.group(3) ?: "" // Puerto de la URL base, si existe
    def registryRepository = baseMatcher.group(4) // uy-com-geocom-geosalud/geosalud-registry/pentaho-server
    def imageVersion = baseMatcher.group(5) // pentaho-server-ce-9.3.0.0-428

    // Construir y retornar un mapa con los componentes separados
    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl, // Directamente usamos la nueva URL base proporcionada
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        newCompleteUrl: "${newRegistryUrl}/${registryRepository}:${imageVersion}" // Construir la nueva URL completa
    ]

    return resultMap
}

// Prueba de la función
def baseRegistryUrl = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrl = "registry-pivot.geocom.com.uy:5005/devops/geometas"

println(newBaseUrl(baseRegistryUrl, newRegistryUrl))
