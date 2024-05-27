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

def getBranches(int repoId, String token) {
    def baseUrl = "https://gitlab.geocom.com.uy/api/v4/projects/${repoId}/repository/branches?per_page=100"
    def command = ["curl", "-s", "-H", "PRIVATE-TOKEN: ${token}", "${baseUrl}"]
    def process = command.execute()
    process.waitFor()
    def output = process.text
    if (output) {
        def json = new JsonSlurper().parseText(output)
        List<String> branches = json.collect { it.name }
        println "Branches encontradas:\n${branches.join('\n')}"
        return branches
    } else {
        println "No se recibió respuesta del servidor. Verificar token, permisos, y conectividad."
        return []
    }
}

def token = lookupCredentials('TEST_GITLAB_TOKEN')
def repoId = 3039
def branches = getBranches(repoId, token)
