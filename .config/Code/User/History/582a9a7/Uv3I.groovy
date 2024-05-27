import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URL
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins
import org.apache.commons.lang.StringEscapeUtils

def lookupCredentials(String id) {
    def creds = CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
        Jenkins.instance,
        null,
        null
    ).find { it.id == id }
    return creds?.password?.getPlainText()
}

def getGitLabRepository(String repoId, String authToken) {
    String gitLabApiUrl = "https://gitlab.geocom.com.uy/api/v4/projects/${repoId}"

    try {
        URL url = new URL(gitLabApiUrl)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.setRequestMethod("GET")
        connection.setRequestProperty("PRIVATE-TOKEN", authToken)
        connection.connect()

        int responseCode = connection.getResponseCode()
        if (responseCode != 200) {
            println "Error al solicitar el repositorio: Código de respuesta HTTP: ${responseCode}"
            return null
        }

        JsonSlurper jsonSlurper = new JsonSlurper()
        def response = jsonSlurper.parseText(connection.getInputStream().getText())
        return response
    } catch (Exception e) {
        println "Error al conectarse a GitLab: ${e.message}"
        return null
    }
}

/////////////////////////////////////////////////////
String repoId = repo_id as int
def token = lookupCredentials('TEST_GITLAB_TOKEN')
def repoData = getGitLabRepository(repoId, token)
def repoUrl = []
String decodedUrl = StringEscapeUtils.unescapeHtml(repoData.ssh_url_to_repo.toString())
repoUrl << decodedUrl
println "Datos del Repositorio: ${repoUrl}"
return repoUrl