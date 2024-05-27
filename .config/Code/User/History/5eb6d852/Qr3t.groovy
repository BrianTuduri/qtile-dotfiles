import groovy.json.JsonSlurper
import groovyx.gpars.GParsPool

String token = "glpat-sfFh_2oi4nixz7c-cxop"  // Set your private token here

int idRepo = 248  // Repository ID
def branches = getBranches(idRepo, token)

def getBranches(int idRepo, String token) {
    def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
    List<String> branches = []
    StringBuilder allBranches = new StringBuilder()
    try {
        // Perform the first page fetch to get headers
        def command = ["curl", "--header", "PRIVATE-TOKEN: ${token}", "--url", url + "&page=1"]
        def process = command.execute()
        def response = process.text
        def headers = process.err.text
        int totalPages = 1

        headers.eachLine {
            if (it.toLowerCase().contains('x-total-pages:')) {
                totalPages = it.split(':')[1].trim() as int
            }
        }
        
        def branchesJson = new JsonSlurper().parseText(response)
        branches += branchesJson.collect { it.name }

        // Use GPars to parallelize fetching of remaining pages
        GParsPool.withPool {
            (2..totalPages).eachParallel { page ->
                def pageCommand = command[0..-2] + ["--url", url + "&page=${page}"]
                def pageResponse = pageCommand.execute().text
                def pageBranches = new JsonSlurper().parseText(pageResponse)
                synchronized (branches) {
                    branches += pageBranches.collect { it.name }
                }
            }
        }

        branches.each { branch -> allBranches.append(branch).append('\n') }
        println "Fetched branches:\n${allBranches.toString()}"
        return branches
    } catch (Exception e) {
        println "An error occurred: ${e.message}"
        return null
    }
}
