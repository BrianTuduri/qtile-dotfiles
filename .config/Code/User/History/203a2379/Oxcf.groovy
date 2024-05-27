def urlClient
def baseRegistryUrl
def newRegistryUrl
def registryRepository
def imageVersion
def newCompleteUrl
def gitLabPath
def newGitLabPath
def gitLabBaseDomain
def newGitLabBaseDomain
def CONFIG = [
    DOCKER_IP_CREDENTIAL: 'docker_node_geocom',
    DOCKER_SERVER_CREDENTIAL: 'docker_slave_credential'
]

pipeline {
    agent { label 'openfortivpn' }
    options {
        ansiColor('xterm')
        quietPeriod(5)
    }
    environment {
        GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no"
    }
    parameters {
        // registry
        string(name: 'registry_url_local', defaultValue: 'registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428', description: 'URL del repositorio de imagenes completa con version (LOCAL) Ejemplo: registry.gitlab.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428') // local & requerido
        string(name: 'registry_local_credentials', defaultValue: 'TOKEN_GITLAB_GEOCOM_MIGRACIONES', description: 'ID de credenciales en JENKINS de la VPN. (LOCAL) (En caso de que se necesite).') // VPN
        string(name: 'gitlab_local_repo_url', defaultValue: 'https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git', description: 'URL del proyecto en Gitlab (LOCAL) Ejemplo: https://gitlab.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git') // local & requerido
        string(name: 'registry_url_remote', defaultValue: 'registry-pivot.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428', description: 'URL del repositorio de imagenes completa con version (REMOTO) Ejemplo: registry-pivot.geocom.com.uy:5005/uy-com-geocom-geosalud/geosalud-registry/pentaho-server:pentaho-server-ce-9.3.0.0-428') // local & requerido
        string(name: 'registry_remote_credentials', defaultValue: 'TOKEN_GITLAB-PIVOT_GEOCOM_MIGRACIONES', description: 'ID de credenciales en JENKINS de la VPN. (REMOTO) (En caso de que se necesite).') // VPN
        string(name: 'gitlab_remote_repo_url', defaultValue: 'https://gitlab-pivot.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git', description: 'URL del proyecto en Gitlab (REMOTO) Ejemplo: https://gitlab-pivot.geocom.com.uy/uy-com-geocom-geosalud/geosalud-registry.git') // local & requerido

        // vpn
        booleanParam(name: 'use_vpn', defaultValue: false, description: 'Indica si necesita una conexion de VPN para llegar a su repositorio GITLAB. En caso de que este sea marcado, debe completar los campos correspondientes. (vpn_host, vpn_port, vpn_credentials)')
        booleanParam(name: 'use_otp', defaultValue: false, description: 'Indica si necesita introducir un codigo OTP de VPN para realizar la conexion VPN).')
        string(name: 'vpn_host', defaultValue: '', description: 'Host de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_port', defaultValue: '', description: 'Puerto de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_credentials', defaultValue: '', description: 'ID de credenciales en JENKINS de la VPN. (En caso de que se necesite).') // VPN
    }
    stages {
        stage('Validate parameters . . .') {
            steps {
                script {
                    // required parameters
                    def requiredParams = [
                        ['paramName': 'registry_url_local', 'paramValue': params.registry_url_local, 'errorMessage': 'Local registry URL cannot be empty or null.'],
                        ['paramName': 'registry_local_credentials', 'paramValue': params.registry_local_credentials, 'errorMessage': 'Local registry credentials cannot be empty or null.'],
                        ['paramName': 'gitlab_local_repo_url', 'paramValue': params.gitlab_local_repo_url, 'errorMessage': 'Local GitLab repository URL cannot be empty or null.'],
                        ['paramName': 'registry_url_remote', 'paramValue': params.registry_url_remote, 'errorMessage': 'Remote registry URL cannot be empty or null.'],
                        ['paramName': 'registry_remote_credentials', 'paramValue': params.registry_remote_credentials, 'errorMessage': 'Remote registry credentials cannot be empty or null.'],
                        ['paramName': 'gitlab_remote_repo_url', 'paramValue': params.gitlab_remote_repo_url, 'errorMessage': 'Remote GitLab repository URL cannot be empty or null.']
                    ]

                    requiredParams.each { param ->
                        if (param.paramValue == null || param.paramValue.trim().isEmpty()) {
                            error(param.errorMessage)
                        }
                    }
                }
            }
        }
        stage(' Establecer conexión con la VPN . . .') {
            when {
                expression {
                    return params.use_vpn == true
                }
            }
            steps {
                script {
                    if (!params.vpn_host.trim() || !params.vpn_port.trim() || !params.vpn_credentials.trim() || !params.gitlab_remote_credentials.trim()) {
                        error("Cuando se utiliza VPN, los campos [vpn_host, vpn_port, vpn_credentials, gitlab_remote_credentials] deben estar completos.")
                    }
                    withCredentials([usernamePassword(credentialsId: params.vpn_credentials, usernameVariable: 'VPN_USERNAME', passwordVariable: 'VPN_PASSWORD')]) {
                        println("Execute openfortivpn . . .")
                        try {
                            def dataExport = "export VPN_HOST=${params.vpn_host} && export VPN_PORT=${params.vpn_port}" 
                            if (params.use_otp) {
                                def otpInput = input(id: 'userInput', message: 'Ingrese el código OTP de su aplicación', parameters: [string(name: 'OTP', defaultValue: '', description: 'Código OTP')])
                                otpInput ?: error("El código OTP no puede estar vacio")
                                dataExport += " export VPN_OTP=${otpInput}"
                            }
                            sh "${dataExport} && trusted_openfortivpn.sh"
                        } catch(Exception e) {
                            def log = sh(script: "cat /tmp/openfortivpn.log", returnStdout: true)
                            error("LOG: ${log} y el error ${e}")
                        }
                    }
                }
            }
        }
        stage('Checkout utils scripts . . .'){
            steps{
                script {
                    git credentialsId: '15940393-32ad-416e-ad80-b8ea71536641', url: 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/scm-pipelines.git'
                }
            }
        }
        stage('Setup environment variables . . .') {
            steps {
                script {
                    def result = newBaseUrl(params.registry_url_local, params.registry_url_remote, params.gitlab_local_repo_url, params.gitlab_remote_repo_url)
                    
                    baseRegistryUrl = result["baseRegistryUrl"]
                    newRegistryUrl = result["newRegistryUrl"]
                    registryRepository = result["registryRepository"]
                    imageVersion = result["imageVersion"]
                    newCompleteUrl = "${result["newRegistryUrl"]}:${result["imageVersion"]}"
                    gitLabPath = result["gitLabPath"]
                    newGitLabPath = result["newGitLabPath"]
                    gitLabBaseDomain = result['gitLabBaseDomain']
                    newGitLabBaseDomain = result['newGitLabBaseDomain']

                    println("""
                    \n\n
                    La URL base de registry es: ${baseRegistryUrl}
                    La URL nueva de registry es: ${newRegistryUrl}
                    El repositorio de registry es: ${registryRepository}
                    La versión de la imagen es: ${imageVersion}
                    La URL nueva completa es: ${newCompleteUrl}
                    El path del repositorio de GitLab es: ${gitLabPath}
                    El nuevo path del repositorio de GitLab es: ${newGitLabPath}
                    La URL base de GitLab es: ${gitLabBaseDomain}
                    La nueva URL base de GitLab es: ${newGitLabBaseDomain}
                    \n\n
                    """)
                }
            }
        }
        stage('Config connection to Gitlab. . .'){
            steps{
                script {
                    withCredentials([usernamePassword(credentialsId: params.registry_remote_credentials, usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_TOKEN')]) {
                        def getGitlabConfig = {
                            return """
                            [global]
                            default=geocom
                            ssl_verify=true
                            timeout=5000
                            [geocom]
                            url=${newGitLabBaseDomain}
                            private_token=${REGISTRY_TOKEN}
                            """
                        }
                    }
                    writeFile file: '/etc/python-gitlab.cfg', text: getGitlabConfig()
                }
            }
        }
        
        stage('Bajar imagen registry de origen y crear tag [LOCAL]...') {
            steps {
                script {
                    withCredentials(usernameColonPassword(credentialsId: CONFIG["DOCKER_IP_CREDENTIAL"], variable: 'IP_PORT')) {
                        docker.withTool('docker') {
                            docker.withServer(IP_PORT, CONFIG["DOCKER_SERVER_CREDENTIAL"]) {
                                docker.withRegistry(baseRegistryUrl, registry_local_credentials) {
                                    dockerLogin(baseRegistryUrl, REGISTRY_USER, REGISTRY_PASS)
                                    dockerPull("${params.registry_url_local}")
                                    dockerTag("${params.registry_url_local}", "${newCompleteUrl}")
                                    executeDockerCommand("images")
                                    dockerLogout(baseRegistryUrl)
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Subiendo imagen a registry remoto docker [REMOTO]. . .') {
            steps {
                script {
                    def executeCommand = { command ->
                        def process = command.execute()
                        process.waitFor()
                        def output = process.text
                        return output
                    }
                    
                    withCredentials([usernamePassword(credentialsId: params.registry_remote_credentials, usernameVariable: 'GITLAB_USER', passwordVariable: 'GITLAB_TOKEN')]) {
                        def outPut = executeCommand("python3 test/gitlab_management.py '${newGitLabBaseDomain}' '${GITLAB_TOKEN}' '${newGitLabPath}'")
                    }

                    withCredentials(usernameColonPassword(credentialsId: CONFIG["DOCKER_IP_CREDENTIAL"], variable: 'IP_PORT')) {
                        docker.withTool('docker') {
                            docker.withServer(IP_PORT, CONFIG["DOCKER_SERVER_CREDENTIAL"]) {
                                docker.withRegistry(newRegistryUrl, registry_remote_credentials) {
                                    dockerLogin(newRegistryUrl, REGISTRY_USER, REGISTRY_PASS)
                                    dockerPush("${newCompleteUrl}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

def executeDockerCommand(String command) {
    sh "docker ${command}"
}

def dockerLogin(String registryUrl, String user, String pass) {
    executeDockerCommand("login ${registryUrl} -u ${user} -p ${pass}")
}

def dockerPull(String imageUrl) {
    executeDockerCommand("pull ${imageUrl}")
}
def dockerPush(String imageUrl) {
    executeDockerCommand("pull ${imageUrl}")
}

def dockerTag(String sourceImage, String targetImage) {
    executeDockerCommand("tag ${sourceImage} ${targetImage}")
}

def dockerLogout(String registryUrl) {
    executeDockerCommand("logout ${registryUrl}")
}

def newBaseUrl(String baseRegistryUrl, String newRegistryUrl, String gitLabRepoUrl, String newGitLabRepoUrl) {
    def basePattern = /^(.*:\/\/)?([^:\/]+)(:\d+)?\/([^\/]+\/[^:]+):([^:]+)$/
    def baseMatcher = (baseRegistryUrl =~ basePattern)
    if (!baseMatcher.matches()) {
        println("La URL base proporcionada no sigue el formato esperado: $baseRegistryUrl")
        return null
    }
    def domain = baseMatcher.group(2)
    def port = baseMatcher.group(3) ?: ""
    def registryRepository = baseMatcher.group(4)
    def imageVersion = baseMatcher.group(5)

    def gitLabPattern = /^(https?:\/\/)([^\/]+)\/(.+?)(\.git)?$/
    def gitLabMatcher = (gitLabRepoUrl =~ gitLabPattern)
    def gitLabBaseDomain = ""
    def gitLabPath = ""
    if (gitLabMatcher.matches()) {
        gitLabBaseDomain = "${gitLabMatcher.group(1)}${gitLabMatcher.group(2)}"
        gitLabPath = gitLabMatcher.group(3)
    } else {
        println("Error en la URL del repositorio de GitLab: $gitLabRepoUrl")
    }

    def newGitLabMatcher = (newGitLabRepoUrl =~ gitLabPattern)
    def newGitLabBaseDomain = ""
    def newGitLabPath = ""
    if (newGitLabMatcher.matches()) {
        newGitLabBaseDomain = "${newGitLabMatcher.group(1)}${newGitLabMatcher.group(2)}"
        newGitLabPath = newGitLabMatcher.group(3)
    } else {
        println("Error en la nueva URL del repositorio de GitLab: $newGitLabRepoUrl")
    }

    def resultMap = [
        baseRegistryUrl: "${domain}${port}",
        newRegistryUrl: newRegistryUrl,
        registryRepository: registryRepository,
        imageVersion: imageVersion,
        gitLabBaseDomain: gitLabBaseDomain,
        gitLabPath: gitLabPath,
        newGitLabBaseDomain: newGitLabBaseDomain,
        newGitLabPath: newGitLabPath,
        newCompleteUrl: "${newRegistryUrl}:${imageVersion}"
    ]

    return resultMap
}