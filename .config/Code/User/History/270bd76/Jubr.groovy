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
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        gitLabBaseDomain: gitLabBaseDomain,
        gitLabPath: gitLabPath,
        newGitLabBaseDomain: newGitLabBaseDomain,
        newGitLabPath: newGitLabPath,
        newCompleteUrl: "${newRegistryUrl}"
    ]

    return resultMap
}

// Ejemplo de uso
def baseRegistryUrlParam = "https://registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrlParam = "https://registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"
def gitLabRepoUrlParam = "https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"
def newGitLabRepoUrlParam = "https://git-pivot.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"

def result = newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam, gitLabRepoUrlParam, newGitLabRepoUrlParam)

def baseRegistryUrl = result["baseRegistryUrl"]
def newRegistryUrl = result["newRegistryUrl"]
def registryRepository = result["registryRepository"]
def imageVersion = result["imageVersion"]
def gitLabPath = result["gitLabPath"]
def newGitLabPath = result["newGitLabPath"]
def newCompleteUrl = "${result["newRegistryUrl"]}:${result["imageVersion"]}"
def gitLabBaseDomain = result["gitLabBaseDomain"]
def newGitLabBaseDomain = result["newGitLabBaseDomain"]

println("""
\n\n
La URL base de registry es: ${baseRegistryUrl}
La URL nueva de registry es: ${newRegistryUrl}
El repositorio de registry es: ${registryRepository}
La versión de la imagen es: ${imageVersion}
El path del repositorio de GitLab es: ${gitLabPath}
El nuevo path del repositorio de GitLab es: ${newGitLabPath}
La URL nueva completa es: ${newCompleteUrl}
La URL base de GitLab es: ${gitLabBaseDomain}
La nueva URL base de GitLab es: ${newGitLabBaseDomain}
\n\n
""")
