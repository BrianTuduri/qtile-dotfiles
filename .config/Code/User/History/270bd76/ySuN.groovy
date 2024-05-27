// Método para determinar si la URL es de un repositorio de GitLab
boolean isGitlabRepository(String url) {
    // Un ejemplo simple de cómo podría ser una URL de GitLab
    def gitlabPattern = /^https:\/\/gitlab\.[^\/]+\/.*\.git$/
    return url ==~ gitlabPattern
}

// Método mejorado para procesar URLs de registry o GitLab
def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
    if (isGitlabRepository(baseRegistryUrl)) {
        println("Detectada URL de repositorio GitLab: $baseRegistryUrl")
        // Lógica específica para GitLab si es necesario
        return [
            originalUrl: baseRegistryUrl,
            newUrl: newRegistryUrl,
            info: "URL de GitLab procesada"
        ]
    } else {
        // Lógica para URLs de registry
        def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
        def baseMatcher = (baseRegistryUrl =~ basePattern)
        if (!baseMatcher.matches()) {
            println("La URL base proporcionada no sigue el formato esperado.")
            return null
        }
        def domain = baseMatcher.group(2)
        def port = baseMatcher.group(3) ?: ""
        def registryRepository = baseMatcher.group(4)
        def imageVersion = baseMatcher.group(5)

        return [
            baseRegistryUrl: "${domain}${port}",
            newRegistryUrl: newRegistryUrl,
            registryRepository: registryRepository,
            imageVersion: imageVersion,
            newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
        ]
    }
}

// Test con una URL de GitLab
def gitlabUrl = "https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"
println(newBaseUrl(gitlabUrl, "https://new.gitlab.com/project/repo.git"))

// Test con una URL de registry
def baseRegistryUrlParam = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrlParam = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"
println(newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam))