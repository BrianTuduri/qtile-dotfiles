import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import groovy.json.JsonSlurper
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins

def lookupCredentials(String id) {
    def creds = CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
        Jenkins.instance,
        null,
        null
    ).find { it.id == id }
    return creds?.password?.getPlainText()
}

def getRepoObject(String projectPathWithNamespace, String token) {
    if (projectPathWithNamespace == null || projectPathWithNamespace.isEmpty()) {
        println "Debe pasar el path_with_namespace del proyecto a la función getRepoObject."
        return null
    }

    try {
        String encodedPath = URLEncoder.encode(projectPathWithNamespace, "UTF-8")
        String urlString = "https://gitlab.geocom.com.uy/api/v4/projects/${encodedPath}"
        URL url = new URL(urlString)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.setRequestMethod("GET")
        connection.setRequestProperty("PRIVATE-TOKEN", token)
        connection.connect()

        int responseCode = connection.getResponseCode()
        if (responseCode != 200) {
            println "Error al solicitar el repositorio: Código de respuesta HTTP: ${responseCode}"
            return null
        }

        JsonSlurper jsonSlurper = new JsonSlurper()
        def projectObject = jsonSlurper.parseText(connection.getInputStream().getText())

        if (projectObject.path_with_namespace.equals(projectPathWithNamespace)) {
            return projectObject
        } else {
            println "No se encontró el proyecto con el path_with_namespace especificado [${projectPathWithNamespace}]."
            return null
        }
    } catch (Exception e) {
        println "Hubo un problema: ${e.message}"
        return null
    }
}

String projectPathWithNamespace = proyecto
def token = lookupCredentials('TEST_GITLAB_TOKEN')

def repoObject = getRepoObject(projectPathWithNamespace, token)
List<Integer> repo_id = []
if (repoObject?.id) {
    repo_id = [repoObject.id]
}
println "\n repo_id: " + repo_id + "\n"
return repo_id