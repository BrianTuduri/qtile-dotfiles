import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.regex.Pattern

def gitlabApi = 'https://gitlab.geocom.com.uy/api/v4'
def projectId = '2739'
def filePath = 'inventory/gama_certificacion'
def personalAccessToken = 'ACCESS_TOKEN'
def branch = 'master'

def getInventoryFile(String gitlabApi, String projectId, String filePath, String personalAccessToken, String branch) {
    def encodedFilePath = URLEncoder.encode(filePath, 'UTF-8')
    def urlString = "${gitlabApi}/projects/${projectId}/repository/files/${encodedFilePath}/raw?ref=${branch}"
    def url = new URL(urlString)
    def connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = 'GET'
    connection.setRequestProperty('PRIVATE-TOKEN', personalAccessToken)

    connection.connect()
    def responseCode = connection.responseCode

    if (responseCode == 200) {
        return connection.inputStream.text
    } else {
        throw new RuntimeException("Failed: HTTP error code: $responseCode")
    }
}

def parseAnsibleGroups(String inventoryData) {
    def groups = []
    def pattern = Pattern.compile(/^\[([^]]+)\]\s*$/, Pattern.MULTILINE)
    def matcher = pattern.matcher(inventoryData)

    while (matcher.find()) {
        String group = matcher.group(1).trim()
        if (!group.contains(':children') && !group.contains(':vars')) {
            groups.add(group)
        }
    }

    return groups.unique()
}

try {
    def inventoryData = getInventoryFile(gitlabApi, projectId, filePath, personalAccessToken, branch)
    def groups = parseAnsibleGroups(inventoryData)
    println JsonOutput.toJson(groups)
    return groups
} catch (Exception e) {
    println "Error: ${e.message}"
    e.printStackTrace()
}