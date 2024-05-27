def newBaseUrl(String baseRegistryUrl, String newRegistryUrl, String gitLabRepoUrl) {
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

    // Extraer el path del repositorio de GitLab
    def gitLabPattern = /^(.*:\/\/)?([^\/]+)\/(.+?)(\.git)?$/
    def gitLabMatcher = (gitLabRepoUrl =~ gitLabPattern)
    def pathRepository = ""
    if (gitLabMatcher.matches()) {
        pathRepository = gitLabMatcher.group(3)
    } else {
        println("La URL del repositorio de GitLab no sigue el formato esperado.")
    }

    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        pathRepository: pathRepository,
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}
// test
def baseRegistryUrlParam = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrlParam = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"
def gitLabRepoUrlParam = "https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git"

println(newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam, gitLabRepoUrlParam))

def result = newBaseUrl(baseRegistryUrlParam, newRegistryUrlParam, gitLabRepoUrlParam)

def baseRegistryUrl = result["baseRegistryUrl"]
def newRegistryUrl = result["newRegistryUrl"]
def registryRepository = result["registryRepository"]
def imageVersion = result["imageVersion"]
def pathRepository = result["pathRepository"]
def newCompleteUrl = "${result["newRegistryUrl"]}:${result["imageVersion"]}"

println("""
\n\n
La URL base de registry es: ${baseRegistryUrl}
La URL nueva de registry es: ${newRegistryUrl}
El repositorio de registry es: ${registryRepository}
La versión de la imagen es: ${imageVersion}
El path del repositorio de GitLab es: ${pathRepository}
La URL nueva completa es: ${newCompleteUrl}
\n\n
""")
