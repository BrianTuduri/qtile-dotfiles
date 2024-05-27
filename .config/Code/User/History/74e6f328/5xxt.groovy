def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
    // Actualizar el patrón para manejar URLs con y sin puerto
    def pattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^:]+):([^:]+)$/
    def matcher = (baseRegistryUrl =~ pattern)
    if (!matcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado.")
        return
    }
    def domain = matcher.group(2) // Dominio sin puerto
    def port = matcher.group(3) ?: "" // Puerto, si existe
    def registryRepository = matcher.group(4) // uy-com-geocom-geosalud/geosalud-registry/pentaho-server
    def imageVersion = matcher.group(5) // pentaho-server-ce-9.3.0.0-428

    // Extraer dominio y puerto (si existe) de newRegistryUrl para mantener consistencia
    def newMatcher = (newRegistryUrl =~ pattern)
    if (!newMatcher.matches()) {
        println("La URL nueva proporcionada no sigue el formato esperado.")
        return
    }
    def newDomain = newMatcher.group(2) // Nuevo dominio sin puerto
    def newPort = newMatcher.group(3) ?: "" // Nuevo puerto, si existe

    // Construir y retornar un mapa con los componentes separados
    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: "${newDomain}${newPort}",
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        newCompleteUrl: "${newRegistryUrl}/${registryRepository}:${imageVersion}"
    ]

    return resultMap
}

// Prueba de la función
def baseRegistryUrl = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrl = "registry-pivot.geocom.com.uy:5005/devops/geometas"

println(newBaseUrl(baseRegistryUrl, newRegistryUrl))
