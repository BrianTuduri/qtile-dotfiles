def newBaseUrl(String baseRegistryUrl, String newRegistryUrl, String gitLabRepoUrl, String newGitLabRepoUrl) {
    def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
    def baseMatcher = (baseRegistryUrl =~ basePattern)
    if (!baseMatcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado: $baseRegistryUrl")
        return null
    }
    def domain = baseMatcher.group(2)
    def port = baseMatcher.group(3) ?: ""
    def registryRepository = baseMatcher.group(4)
    def imageVersion = baseMatcher.group(5)

    // Revisar la expresión regular para que coincida con las URLs de GitLab hasta antes del '.git', incluyendo el esquema
    def gitLabPattern = /^(https?:\/\/[^\/]+)\/.+/
    def gitLabMatcher = (gitLabRepoUrl =~ gitLabPattern)
    def gitLabBaseDomain = ""
    if (gitLabMatcher.matches()) {
        gitLabBaseDomain = gitLabMatcher.group(1)  // Capturar incluyendo 'https://'
    } else {
        println("Error en la URL del repositorio de GitLab: $gitLabRepoUrl")
    }

    // Extraer el nuevo dominio base del repositorio de GitLab utilizando el mismo patrón
    def newGitLabMatcher = (newGitLabRepoUrl =~ gitLabPattern)
    def newGitLabBaseDomain = ""
    if (newGitLabMatcher.matches()) {
        newGitLabBaseDomain = newGitLabMatcher.group(1)  // Capturar incluyendo 'https://'
    } else {
        println("Error en la nueva URL del repositorio de GitLab: $newGitLabRepoUrl")
    }

    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        gitLabBaseDomain: gitLabBaseDomain,
        newGitLabBaseDomain: newGitLabBaseDomain,
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}

// Ejemplo de uso
def baseRegistryUrlParam = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrlParam = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"
def gitLabRepoUrlParam = "https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"
def newGitLabRepoUrlParam = "https://git-pivot.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"

def result = newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam, gitLabRepoUrlParam)

def baseRegistryUrl = result["baseRegistryUrl"]
def newRegistryUrl = result["newRegistryUrl"]
def registryRepository = result["registryRepository"]
def imageVersion = result["imageVersion"]
def pathRepository = result["pathRepository"]
def newCompleteUrl = "${result["newRegistryUrl"]}:${result["imageVersion"]}"
def gitLabBaseDomain = result["gitLabBaseDomain"]
def newGitLabBaseDomain = result["newGitLabBaseDomain"]

println("""
\n\n
La URL base de registry es: ${baseRegistryUrl}
La URL nueva de registry es: ${newRegistryUrl}
El repositorio de registry es: ${registryRepository}
La versión de la imagen es: ${imageVersion}
El path del repositorio de GitLab es: ${pathRepository}
La URL nueva completa es: ${newCompleteUrl}
La URL base de GitLab es: ${gitLabBaseDomain}
La nueva URL base de GitLab es: ${newGitLabBaseDomain}
\n\n
""")
