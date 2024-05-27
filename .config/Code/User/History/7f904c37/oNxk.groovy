def toCheckout
def localCheckout
def pom 
def pomVersion
def isRegenerate = params.tarea == "regenerate_version"
def URL_REPO = "git@gitlab.geocom.com.uy:uy-com-geocom-alkosto/geopos2-alkosto.git"
def baseRepoUrl = URL_REPO.replaceFirst(/\.git$/, "")
def projectName = baseRepoUrl.tokenize('/').last()
def mavenCommands = getMavenCommands(params.tarea)

def CONFIG = [
    DOCKER_IP_CREDENTIAL: 'docker_slave_ip_port',
    DOCKER_SERVER_CREDENTIAL: 'Docker_Certs',
    NEXUS_REPOSITORY: "Ghiggia-60-dias",
    NEXUS_GROUP_ID: "uy.com.geocom.alkosto",
    NEXUS_CREDENTIAL_ID: "nexus-deploy",
    GIT_CREDENTIAL_ID: "15940393-32ad-416e-ad80-b8ea71536641",
    CHECKOUT_FOLDER: "repo",
    LABEL: "jdk21",
    URL_REPO: baseRepoUrl
]

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
        // stage('Checkout . . .') {
        //     steps {
        //         script {
        //             toCheckout = (isRegenerate ? "refs/tags/geopos2-alkosto-${params.version}" : "*/${params.branch}")  
        //             localCheckout = isRegenerate ? params.version : params.branch   
        //             checkout changelog: false, poll: false, scm: scmGit(
        //                 branches: [[name: toCheckout]],
        //                 doGenerateSubmoduleConfigurations: false, 
        //                 extensions: [
        //                     cloneOption(depth: 1, noTags: true, reference: '', shallow: true),
        //                     [$class: 'RelativeTargetDirectory', relativeTargetDir: CONFIG["CHECKOUT_FOLDER"]],
        //                     [$class: 'LocalBranch', localBranch: (localCheckout)]],gitTool: 'jgit', 
        //                 userRemoteConfigs: [[credentialsId: CONFIG["GIT_CREDENTIAL_ID"], url: 'git@gitlab.geocom.com.uy:uy-com-geocom-alkosto/geopos2-alkosto.git']])
        //             
        //             sh '''
        //                 git config --global user.email geoscm@geocom.com.uy
        //                 git config --global user.name 'JenkinsSCM'
        //             '''
        //         }
        //     }
        // }
        
        // stage('Read pom.xml . . .') {
        //     steps {
        //         script {
        //             dir(CONFIG["CHECKOUT_FOLDER"]) {
        //                 pom = readMavenPom file: 'pom.xml'
        //                 pomVersion = pom.getVersion().replaceAll("-SNAPSHOT", "")
        //             }
        //         }
        //     }
        // }
        stage('Ejecucion Maven . . .') {
            steps {
                script {
                    dir(CONFIG["CHECKOUT_FOLDER"]) {
                        configFileProvider([configFile(fileId: 'mvn_local_docker_release', targetLocation: "${env.HOME}/.m2/settings.xml")]) {
                            withDockerEnvironment(CONFIG, 
                                { 
                                    def sshVolume = "/tmp/${UUID.randomUUID()}"
                                    sh "mkdir -p ${sshVolume}"
                                    withCredentials([sshUserPrivateKey(credentialsId: CONFIG["GIT_CREDENTIAL_ID"], keyFileVariable: 'sshKey')]) {
                                        writeFile file: "${sshVolume}/id_rsa", text: readFile("${sshKey}")
                                    }
                                    dockerArgs = ["-u 0", "-v ${MVN_VOLUME}" ,"--add-host 'gitlab.geocom.com.uy:172.24.18.194'"].join(" ")
                                    return runDockerContainer(DOCKER_IMAGE, dockerArgs)
                                },
                                { containerId ->
                                    def shDocker = { command ->
                                        sh "docker exec ${containerId} sh -c '${command}'"
                                    }
                                    mavenArgs = [""].join(" ")
                                    shDocker "mkdir -p /root/.ssh/"
                                    sh "docker cp ${sshVolume}/id_rsa ${containerId}:/root/.ssh/id_rsa"
                                    shDocker "chmod 600 /root/.ssh/id_rsa && ssh-keyscan -H gitlab.geocom.com.uy >> /root/.ssh/known_hosts"
                                    shDocker "git clone ${URL_REPO}"
                                    shDocker "cd ${projectName} && git checkout ${localCheckout} && mvn ${mavenCommands} ${mavenArgs}"
                                }
                            )
                        }
                    }
                }
            }
        }


        // stage('Publicando zip en Nexus') {
        //     when {
        //         expression {
        //             return params.tarea != "build"
        //         }
        //     }
        //     steps {
        //         script {
        //             def zipName = "geopos2-alkosto-${pomVersion}-Package.zip"
        //             dir("${CONFIG["CHECKOUT_FOLDER"]}/target") {
        //                 sh "ls -la ."
        //                 nexusArtifactUploader artifacts: [[artifactId: "geopos2-alkosto", classifier: '', file: zipName, type: 'zip']],
        //                         credentialsId: CONFIG["NEXUS_CREDENTIAL_ID"],
        //                         groupId: CONFIG["NEXUS_GROUP_ID"],
        //                         nexusUrl: 'nexus.geocom.com.uy',
        //                         nexusVersion: 'nexus3',
        //                         protocol: 'https',
        //                         repository: CONFIG["NEXUS_REPOSITORY"],
        //                         version: "${pomVersion}"
        //                         
        //             }
        //         }
        //     }
        // }

        // stage('Iniciar Instalación') {
        //     when {
        //         expression { 
        //             return params.INSTALL && params.tarea != "build"
        //         }
        //     }
        //     steps {
        //         script {
        //             def etiquetasParsed = [];
        //             // full_install_pos, full_install_local, full_install_central
        //             for (et in params.etiquetas.split(',')) {
        //                 etiquetasParsed << "full_install_${et}"
        //             }
        //             println "Etiquetas: ${etiquetasParsed.join(',')}"
        //             build job: 'Install-Alkosto-multihost', parameters: [
        //                 string(name: 'version', value: pomVersion),
        //                 string(name: 'ambiente', value: params.ambiente),
        //                 string(name: 'store', value: params.store),
        //                 string(name: 'particular_host', value: params.particular_host),
        //                 extendedChoice(name: 'etiqueta', value: etiquetasParsed.join(',')),
        //                 booleanParam(name: 'delegate', value: false),
        //                 booleanParam(name: 'validate', value: true),
        //                 booleanParam(name: 'force', value: false),
        //                 booleanParam(name: 'copy_panaso', value: false)
        //             ],quietPeriod: 5, wait: true
        //         }
        //     }
        // }
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

def withDockerEnvironment(config, Closure dockerRunConfig, Closure body) {
    withCredentials([usernameColonPassword(credentialsId: config["DOCKER_IP_CREDENTIAL"], variable: 'IP_PORT'),
        dockerCert(credentialsId: config["DOCKER_SERVER_CREDENTIAL"], variable: 'DOCKER_CERT')]) {
        docker.withTool('docker') {
            docker.withServer(IP_PORT, 'Docker_Certs') {
                String containerId
                try {
                    containerId = dockerRunConfig.call()
                    echo "Contenedor iniciado: ${containerId}"
                    body.call(containerId)
                } finally {
                    if (containerId) {
                        stopAndRemoveDockerContainer(containerId)
                        echo "El contenedor Docker ha sido detenido y eliminado exitosamente."
                    }
                }
            }
        }
    }
}

def runDockerContainer(String dockerImage, String runArguments = '') {
    String dockerRunCommand = "docker run -d ${runArguments} ${dockerImage} tail -f /dev/null"
    return sh(script: dockerRunCommand, returnStdout: true).trim()
}

def stopAndRemoveDockerContainer(String containerId) {
    sh "docker stop ${containerId}; docker rm ${containerId}"
}
