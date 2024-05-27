import groovyx.net.http.HttpBuilder
import static groovyx.net.http.ContentType.JSON
import groovyx.gpars.GParsPool

String token = "glpat-sfFh_2oi4nixz7c-cxop"

int idRepo = 248
def branches = getBranches(idRepo, token)

def getBranches(int idRepo, String token) {
    def url = "https://gitlab.geocom.com.uy/api/v4/projects/${idRepo}/repository/branches?per_page=100"
    def totalPages = 1
    List<String> branches = []

    try {
        // Inicializar HttpBuilder
        def http = HttpBuilder.configure {
            request.uri = url
            request.headers['PRIVATE-TOKEN'] = token
        }

        // Obtener el número total de páginas
        totalPages = http.head {
            request.uri.query = [per_page: 100]
            response.success { resp, reader ->
                resp.headers.each { header ->
                    if (header.key.toLowerCase().contains('x-total-pages')) {
                        totalPages = header.value[0] as int
                    }
                }
            }
        }

        // Uso de paralelismo para fetch de páginas
        GParsPool.withPool {
            (1..totalPages).eachParallel { page ->
                http.get {
                    request.uri.query = [page: page]
                    response.success { resp, json ->
                        branches += json.collect { it.name }
                    }
                }
            }
        }

        println "Fetched branches:\n" + branches.join('\n')
        return branches
    } catch (Exception e) {
        println "An error occurred: ${e.message}"
        return null
    }
}
