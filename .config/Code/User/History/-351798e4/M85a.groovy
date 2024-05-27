    import groovy.json.JsonSlurper
try {
    List<String> artifacts = new ArrayList<String>()
    def artifactsUrl = "https://nexus.geocom.com.uy/service/rest/v1/search?repository=GEOSalud-Wars"
    def artifactsObjectRaw = ["curl", "-s", "-H", "accept: application/json", "-k", "--url", "${artifactsUrl}"].execute().text
    def jsonSlurper = new JsonSlurper()
    def artifactsJsonObject = jsonSlurper.parseText(artifactsObjectRaw)
    println(artifactsJsonObject)
    def projects = ["geopdc_servicios"]
    def dataArray = artifactsJsonObject.items.findAll { 
        projects.contains(it.name)
    }
    for(item in dataArray){
        for (element in item.assets) {
            String p = element.path
            if (p.endsWith(".war")) {
                def finalName = element.path.split('/')
                artifacts.add(finalName[-2] +"/"+ finalName[-1])
            }
        }
    } 
    print artifacts
    return artifacts
} catch (Exception e) {
    print "Hubo un problema reconociendo las versiones..."
    print e
}