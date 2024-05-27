import groovy.json.JsonSlurper 

String token = "TBQpNmvPZfvyAyMJnoA9"
def releaseType = 'path'
if (releaseType.equals("patch") || releaseType.equals("minor") || releaseType.equals("major")) {
    idRepo=705
    branches = getBranches(idRepo, token)
    return branches
} else {
    return ["No selecciono build o release"]
}

def getBranches(int idRepo, String token) {
    try {
        List<String> artifacts = new ArrayList<String>()
        def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
        int totalPages = 1;
        def getTotalPages = ["curl", "--head", "--header", "PRIVATE-TOKEN: ${token}", "--url", url].execute().text.split('\n')
        for (i in getTotalPages) {
            if (i.toLowerCase().contains('x-total-pages')) {
                totalPages = Integer.parseInt(i.split(':')[1].trim())
                break
            }
        }
        for (p in 1..totalPages) {
            curlArray = ["curl", "--header", "PRIVATE-TOKEN: ${token}", "--url", "${url}&page=${p}"]
            def artifactsObjectRaw = curlArray.execute().text
            def artifactsJsonObject = new JsonSlurper().parseText(artifactsObjectRaw)
            println("Artifacts: " + artifactsJsonObject)
            for (item in artifactsJsonObject) {
                if (item.name) {
                    artifacts.add(item.name)
                }
            }
        }
        println artifacts.join('\n')
        return artifacts
    } catch (Exception e) {
        print "Hubo un problema: " + e
    }
}