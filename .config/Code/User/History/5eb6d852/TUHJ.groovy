import groovy.json.JsonSlurper
import groovyx.gpars.GParsPool

String token = "glpat-sfFh_2oi4nixz7c-cxop"  // Set your private token here

int idRepo = 248  // Repository ID
def branches = getBranches(idRepo, token)

def getBranches(int idRepo, String token) {
    def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
    List<String> branches = []
    StringBuilder allBranches = new StringBuilder()
    int totalPages = 1

    try {
        // Execute curl to fetch both headers and the first page of results
        def command = ["curl", "--header", "PRIVATE-TOKEN: ${token}", "--url", "${url}&page=1"]
        def process = command.execute()
        process.waitFor() // Ensure the command completes execution

        if (process.exitValue() != 0) {
            println "Error executing curl command: ${process.err.text}"
            return null
        }

        def response = process.in.text  // Read the response from the standard input stream
        def headers = process.err.text  // Read headers from the error stream

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
                def pageCommand = ["curl", "--header", "PRIVATE-TOKEN: ${token}", "--url", "${url}&page=${page}"]
                def pageProcess = pageCommand.execute()
                pageProcess.waitFor()
                if (pageProcess.exitValue() == 0) {
                    def pageResponse = pageProcess.in.text
                    def pageBranches = new JsonSlurper().parseText(pageResponse)
                    synchronized (branches) {
                        branches += pageBranches.collect { it.name }
                    }
                } else {
                    println "Error fetching page $page: ${pageProcess.err.text}"
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
