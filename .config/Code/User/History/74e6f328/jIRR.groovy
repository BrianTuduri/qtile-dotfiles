def newBaseUrl(String baseRegistryUrl, String newRegistryUrl) {
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
    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}
// test
def baseRegistryUrl = "registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428"
def newRegistryUrl = "registry-pivot.geocom.com.uy:5005/devops/geometas/geosalud-registry/pentaho-server"

println(newBaseUrl(baseRegistryUrl, newRegistryUrl))

def result = newBaseUrl(baseRegistryUrl, newRegistryUrl)

def baseRegistryUrl = result["baseRegistryUrl"]
def newRegistryUrl = result["newRegistryUrl"]
def registryRepository = result["registryRepository"]
def imageVersion = result["imageVersion"]
def newCompleteUrl = "${result.newRegistryUrl}:${result.imageVersion}"

println("""
La URL base de registry es: ${baseRegistryUrl}
La URL nueva de registry es: ${newRegistryUrl}
El repositorio de registry es: ${registryRepository}
La versión de la imagen es: ${imageVersion}
La URL nueva completa es: ${newCompleteUrl}
""")
