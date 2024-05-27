import groovy.json.JsonSlurper

def getBranches(int repoId, String token) {
    def baseUrl = "https://gitlab.geocom.com.uy/api/v4/projects/${repoId}/repository/branches?per_page=100"
    def command = ["curl", "-s", "-H", "PRIVATE-TOKEN: ${token}", "${baseUrl}"]
    print command
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

def token = "glpat-sfFh_2oi4nixz7c-cxop"
def repoId = 248
def branches = getBranches(repoId, token)