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

def getBranches(int idRepo, String token) {
    List<String> branches = []
    def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
    int totalPages = 1
    try {
        def getTotalPages = ["curl", "--head", "--header", "PRIVATE-TOKEN: ${token}", "--url", url].execute()
        getTotalPages.text.eachLine {
            if (it.toLowerCase().contains('x-total-pages:')) {
                totalPages = it.split(':')[1].trim() as int
            }
        }
        
        (1..totalPages).each { page ->
            def curlCmd = ["curl", "--header", "PRIVATE-TOKEN: ${token}", "--url", "${url}&page=${page}"]
            def response = curlCmd.execute().text
            def branchesJson = new JsonSlurper().parseText(response)
            branches += branchesJson.collect { it.name }
        }
        
        println "Fetched branches:\n" + branches.join('\n')
        return branches
    } catch (Exception e) {
        println "An error occurred: ${e.message}"
        return null
    }
}

def token = lookupCredentials('TEST_GITLAB_TOKEN')
def repoId = repo_id as int
def branches = getBranches(repoId, token)