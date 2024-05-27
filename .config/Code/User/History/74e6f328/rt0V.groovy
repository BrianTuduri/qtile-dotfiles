def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
    // Actualizar el patrón para manejar URLs con y sin puerto para la baseRegistryUrl
    def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
    def baseMatcher = (baseRegistryUrl =~ basePattern)
    if (!baseMatcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado.")
        return null
    }
    // Dominio sin puerto de la URL base
    def domain = baseMatcher.group(2)
    // Puerto de la URL base, si existe
    def port = baseMatcher.group(3) ?: ""
    // uy-com-geocom-geosalud/geosalud-registry/pentaho-server
    def registryRepository = baseMatcher.group(4)
    // pentaho-server-ce-9.3.0.0-428
    def imageVersion = baseMatcher.group(5)

    // No es necesario extraer dominio y puerto de newRegistryUrl porque ya viene formateado como parte de la URL

    // Construir y retornar un mapa con los componentes separados. Asegurándonos de que la newCompleteUrl se forma correctamente.
    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl, // Usamos directamente la nueva URL base proporcionada
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        // Asumimos que newRegistryUrl ya incluye la parte necesaria del repositorio, solo agregamos la imagen y versión
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}

// Prueba de la función
def baseRegistryUrl = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrl = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"

println(newBaseUrl(baseRegistryUrl, newRegistryUrl))

def version = newBaseUrl(baseRegistryUrl, newRegistryUrl)["version"]
println version