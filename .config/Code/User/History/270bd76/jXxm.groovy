def newBaseUrl(String baseRegistryUrl, String newRegistryUrl, String gitLabRepoUrl, String newGitLabRepoUrl) {
    def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
    def baseMatcher = (baseRegistryUrl =~ basePattern)
    if (!baseMatcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado: $baseRegistryUrl")
        return null
    }
    def protocol = baseMatcher.group(1) ?: "https:"
    def domain = baseMatcher.group(2)
    def port = baseMatcher.group(3) ?: ""
    def registryRepository = baseMatcher.group(4)
    def imageVersion = baseMatcher.group(5)

    def gitLabPattern = /^(https?:\/\/)([^\/]+)\/(.+?)(\.git)?$/
    def gitLabMatcher = (gitLabRepoUrl =~ gitLabPattern)
    def gitLabBaseDomain = ""
    def gitLabPath = ""
    if (gitLabMatcher.matches()) {
        gitLabBaseDomain = "${gitLabMatcher.group(1)}${gitLabMatcher.group(2)}"
        gitLabPath = gitLabMatcher.group(3)
    } else {
        println("Error en la URL del repositorio de GitLab: $gitLabRepoUrl")
    }

    def newGitLabMatcher = (newGitLabRepoUrl =~ gitLabPattern)
    def newGitLabBaseDomain = ""
    def newGitLabPath = ""
    if (newGitLabMatcher.matches()) {
        newGitLabBaseDomain = "${newGitLabMatcher.group(1)}${newGitLabMatcher.group(2)}"
        newGitLabPath = newGitLabMatcher.group(3)
    } else {
        println("Error en la nueva URL del repositorio de GitLab: $newGitLabRepoUrl")
    }

    def resultMap = [
        protocol: protocol,
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        gitLabBaseDomain: gitLabBaseDomain,
        gitLabPath: gitLabPath,
        newGitLabBaseDomain: newGitLabBaseDomain,
        newGitLabPath: newGitLabPath,
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}

// Ejemplo de uso
def baseRegistryUrlParam = "https://registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrlParam = "https://registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"
def gitLabRepoUrlParam = "https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"
def newGitLabRepoUrlParam = "https://git-pivot.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"

def result = newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam, gitLabRepoUrlParam, newGitLabRepoUrlParam)

println("""
\n\n
Protocolo: ${result["protocol"]}
La URL base de registry es: ${result["baseRegistryUrl"]}
La URL nueva de registry es: ${result["newRegistryUrl"]}
El repositorio de registry es: ${result["registryRepository"]}
La versión de la imagen es: ${result["imageVersion"]}
El path del repositorio de GitLab es: ${result["gitLabPath"]}
El nuevo path del repositorio de GitLab es: ${result["newGitLabPath"]}
La URL nueva completa es: ${result["newCompleteUrl"]}
La URL base de GitLab es: ${result["gitLabBaseDomain"]}
La nueva URL base de GitLab es: ${result["newGitLabBaseDomain"]}
\n\n
""")
