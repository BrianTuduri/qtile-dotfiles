import groovy.json.JsonSlurper

String token = "glpat-sfFh_2oi4nixz7c-cxop"  // Remember to set your GitLab private token here

int idRepo = 248  // Set the ID of your GitLab repository
def branches = getBranches(idRepo, token)

def getBranches(int idRepo, String token) {
    List<String> branches = []
    def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
    int totalPages = 1
    try {
        // Fetch total number of pages
        def getTotalPages = ["curl", "--head", "--header", "PRIVATE-TOKEN: ${token}", "--url", url].execute()
        getTotalPages.text.eachLine {
            if (it.toLowerCase().contains('x-total-pages:')) {
                totalPages = it.split(':')[1].trim() as int
            }
        }
        
        // Fetch branches across all pages
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
