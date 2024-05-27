// Definir containerId como una variable global
def containerId = ""
def toCheckout
def localCheckout
def pom 
def pomVersion
def isRegenerate = params.tarea == "regenerate_version"
def CONFIG = [
    DOCKER_IP_CREDENTIAL: 'docker_slave_ip_port',
    DOCKER_SERVER_CREDENTIAL: 'Docker_Certs',
    NEXUS_REPOSITORY: "Ghiggia-60-dias",
    NEXUS_GROUP_ID: "uy.com.geocom.alkosto",
    NEXUS_CREDENTIAL_ID: "nexus-deploy",
    GIT_CREDENTIAL_ID: "15940393-32ad-416e-ad80-b8ea71536641",
    CHECKOUT_FOLDER: "repo",
    LABEL: "jdk21",
    URL_REPO: "https://gitlab.geocom.com.uy/uy-com-geocom-alkosto/geopos2-alkosto.git".last('/').split('.')[0]
]
def projectName = CONFIG["URL_REPO"].split('/').last().replaceAll(".git", "")
def mavenCommands = getMavenCommands(params.tarea)

pipeline {
    agent { label "${CONFIG["LABEL"]}" }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        quietPeriod(10)
    }
    environment {
        DOCKER_IMAGE = "nexus.geocom.com.uy/scm-docker-images/openjdk:mvn2.2.1-jdk8"
        MVN_VOLUME = "/slave/m2/:/root/.m2/"
    }
    stages {
        stage('Checkout . . .') {
            steps {
                script {
                    toCheckout = (isRegenerate ? "refs/tags/geopos2-alkosto-${params.version}" : "*/${params.branch}")  
                    localCheckout = isRegenerate ? params.version : params.branch   
                    checkout changelog: false, poll: false, scm: scmGit(
                        params.branches: [[name: toCheckout]],
                        doGenerateSubmoduleConfigurations: false, 
                        extensions: [
                            cloneOption(depth: 1, noTags: true, reference: '', shallow: true),
                            [$class: 'RelativeTargetDirectory', relativeTargetDir: CONFIG["CHECKOUT_FOLDER"]],
                            [$class: 'LocalBranch', localparams.branch: (localCheckout)]
                        ], gitTool: 'jgit', userRemoteConfigs: [[credentialsId: CONFIG["GIT_CREDENTIAL_ID"], url: 'git@gitlab.geocom.com.uy:uy-com-geocom-alkosto/geopos2-alkosto.git']])
                    
                    sh '''
                        git config --global user.email geoscm@geocom.com.uy
                        git config --global user.name 'JenkinsSCM'
                    '''
                }
            }
        }
        
        stage('Read pom.xml . . .') {
            steps {
                script {
                    dir(CONFIG["CHECKOUT_FOLDER"]) {
                        pom = readMavenPom file: 'pom.xml'
                        pomVersion = pom.getVersion().replaceAll("-SNAPSHOT", "")
                    }
                }
            }
        }

        stage('Ejecucion Maven') {
            steps {
                script {
                    dir(CONFIG["CHECKOUT_FOLDER"]) {
                        configFileProvider([configFile(fileId: '`', targetLocation: "${env.HOME}/.m2/settings.xml")]) {
                            // sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                            // }   
                            withDockerEnvironment(CONFIG) {
                                containerId = runDockerContainer(DOCKER_IMAGE, "-u 0 -v ${MVN_VOLUME} -e 'MY_VAR=my_value'")
                                def shDocker = { command ->
                                    sh "docker exec ${containerId} sh -c '${command}'"
                                }
                                
                                echo "Contenedor iniciado: ${containerId}"
                                shDocker "mvn --version && cat /root/.m2/settings.xml"
                                shDocker "whoami"
                                //shDocker(containerId, "ls -la /root/.m2")
                                withCredentials([usernamePassword(credentialsId: 'jenkins_deploy_token', passwordVariable: 'TOKEN', usernameVariable: 'GITLAB_USER')]) {

                                    shDocker "git config --global user.email geoscm@geocom.com.uy && git config --global user.name '$GITLAB_USER'"
                                    shDocker "git clone https://oauth2:$TOKEN@gitlab.geocom.com.uy/uy-com-geocom-alkosto/geopos2-alkosto.git"
                                }
                                shDocker "ls -lah ./ && cd ${projectName} && git checkout ${localCheckout} && mvn ${mavenCommands}"
                                stopAndRemoveDockerContainer(containerId)
                            }
                        }
                        
                    }
                }
            }
        }

        stage('Publicando zip en Nexus') {
            when {
                expression {
                    return params.tarea != "build"
                }
            }
            steps {
                script {
                    def zipName = "geopos2-alkosto-${pomVersion}-Package.zip"
                    dir("${CONFIG["CHECKOUT_FOLDER"]}/target") {
                        sh "ls -la ."
                        nexusArtifactUploader artifacts: [[artifactId: "geopos2-alkosto", classifier: '', file: zipName, type: 'zip']],
                                credentialsId: CONFIG["NEXUS_CREDENTIAL_ID"],
                                groupId: CONFIG["NEXUS_GROUP_ID"],
                                nexusUrl: 'nexus.geocom.com.uy',
                                nexusVersion: 'nexus3',
                                protocol: 'https',
                                repository: CONFIG["NEXUS_REPOSITORY"],
                                version: "${pomVersion}"
                                
                    }
                }
            }
        }

        stage('Iniciar Instalación') {
            when {
                expression { 
                    return params.INSTALL && params.tarea != "build"
                }
            }
            steps {
                script {
                    def etiquetasParsed = [];
                    // full_install_pos, full_install_local, full_install_central
                    for (et in params.etiquetas.split(',')) {
                        etiquetasParsed << "full_install_${et}"
                    }
                    println "Etiquetas: ${etiquetasParsed.join(',')}"
                    build job: 'Install-Alkosto-multihost', parameters: [
                        string(name: 'version', value: pomVersion),
                        string(name: 'ambiente', value: params.ambiente),
                        string(name: 'store', value: params.store),
                        string(name: 'particular_host', value: params.particular_host),
                        extendedChoice(name: 'etiqueta', value: etiquetasParsed.join(',')),
                        booleanParam(name: 'delegate', value: false),
                        booleanParam(name: 'validate', value: true),
                        booleanParam(name: 'force', value: false),
                        booleanParam(name: 'copy_panaso', value: false)
                    ],quietPeriod: 5, wait: true
                }
            }
        }
    }
}

def getMavenCommands(String tarea) {
    switch(tarea) {
        case 'release':
            return "-Dresume=false release:prepare release:perform"
        case 'regenerate_version':
            return "clean package"
        default:
            return "clean deploy"
    }
}

def withDockerEnvironment(config, Closure body) {
    withCredentials([usernameColonPassword(credentialsId: config["DOCKER_IP_CREDENTIAL"], variable: 'IP_PORT'),
                    dockerCert(credentialsId: config["DOCKER_SERVER_CREDENTIAL"], variable: 'DOCKER_CERT')]) {
        docker.withTool('docker') {
            docker.withServer(IP_PORT, 'Docker_Certs') {
                body.call()
            }
        }
    }
}

def runDockerContainer(String dockerImage, String runArguments = '') {
    String dockerRunCommand = "docker run -d ${runArguments} ${dockerImage} tail -f /dev/null"
    return sh(script: dockerRunCommand, returnStdout: true).trim()
}

def stopAndRemoveDockerContainer(String containerId) {
    sh "docker stop ${containerId}"
    sh "docker rm ${containerId}"
}

