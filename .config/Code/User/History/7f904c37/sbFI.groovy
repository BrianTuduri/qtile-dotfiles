def pom 
def pomVersion
def BRANCH = "master"
def isRegenerate = params.tarea == "regenerate_version"
def checkoutFolder = "repo"
def REPO_PATH
def PROJECT_VOLUME
def CONFIG = [
    DOCKER_IP_CREDENTIAL: 'docker_slave_ip_port',
    DOCKER_SERVER_CREDENTIAL: 'Docker_Certs',
    NEXUS_REPOSITORY: "Ghiggia-60-dias",
    NEXUS_GROUP_ID: "uy.com.geocom.alkosto",
    NEXUS_CREDENTIAL_ID: "nexus-deploy",
    GIT_CREDENTIAL_ID: "15940393-32ad-416e-ad80-b8ea71536641",
    LABEL: "jdk21",
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
        stage('Checkout . . .') {
            steps {
                script {
                    REPO_PATH = "${env.WORKSPACE}/${checkoutFolder}"
                    PROJECT_VOLUME = "${REPO_PATH}:/project"
                    sh """
                    echo Repo path is: ${REPO_PATH}
                    echo Docker image is: ${env.DOCKER_IMAGE}
                    echo MVN Volume is: ${env.MVN_VOLUME}
                    echo Project Volume is: ${PROJECT_VOLUME}
                    """
                    def toCheckout = (isRegenerate ? "refs/tags/geopos2-alkosto-${params.version}" : "*/${BRANCH}")
                    checkout([$class                           : 'GitSCM',
                        branches                         : [[name: toCheckout]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: checkoutFolder],
                                                            [$class: 'LocalBranch', localBranch: (isRegenerate ? params.version : BRANCH)]],
                        gitTool                          : 'Default',
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [[credentialsId: CONFIG["GIT_CREDENTIAL_ID"],
                                                            url          : 'git@gitlab.geocom.com.uy:uy-com-geocom-alkosto/geopos2-alkosto.git']]])
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
                    dir(checkoutFolder) {
                        pom = readMavenPom file: 'pom.xml'
                        pomVersion = pom.getVersion().replaceAll("-SNAPSHOT", "")
                    }
                }
            }
        }

        stage('Ejecucion Maven') {
            steps {
                script {
                    dir(checkoutFolder) {
                        configFileProvider([configFile(fileId: 'af5f8d3f-79e7-4ed3-8241-c0b831982733', targetLocation: "${env.HOME}/.m2/settings.xml")]) {
                            sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                                runMavenTaskInDocker(getMavenCommands(params.tarea), DOCKER_IMAGE, MVN_VOLUME, PROJECT_VOLUME, CONFIG["DOCKER_IP_CREDENTIAL"],CONFIG["DOCKER_SERVER_CREDENTIAL"])
                                //testRunImage(CONFIG["DOCKER_IP_CREDENTIAL"], CONFIG["DOCKER_SERVER_CREDENTIAL"])
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
                    dir("${checkoutFolder}/target") {
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

def runMavenTaskInDocker(String tarea, String dockerImage, String mvnVolume, String projectVolume, String docker_ip_credential, docker_server_credential) {
    def mavenCommands = getMavenCommands(tarea)
    withCredentials([usernameColonPassword(credentialsId: docker_ip_credential, variable: 'IP_PORT')]) {
        withCredentials([dockerCert(credentialsId: docker_server_credential, variable: 'DOCKER_CERT')]) {
            docker.withServer(IP_PORT, 'Docker_Certs') {
                docker.image(dockerImage).withRun("-v ${mvnVolume}") {
                    withCredentials([usernamePassword(credentialsId: 'jenkins_deploy_token', passwordVariable: 'TOKEN', usernameVariable: 'GITLAB_USER')]) {
                        
                        // DEBUG
                        sh "echo Verificando la disponibilidad de Maven && mvn -version"
                        sh "echo Imprimiendo variables de entorno && env"
                        sh "echo Listando el contenido del directorio Maven && ls -la /usr/local/maven/bin"
                        // DEBUG
                        
                        sh 
                        sh """
                            git config --global user.email geoscm@geocom.com.uy
                            git config --global user.name '$GITLAB_USER'
                            git clone https://oauth2:$TOKEN@gitlab.geocom.com.uy/uy-com-geocom-alkosto/geopos2-alkosto.git 
                        """
                        //    git remote set-url origin https://oauth2:$TOKEN@gitlab.geocom.com.uy/$GITLAB_USER/uy-com-geocom-alkosto/geopos2-alkosto.git
                    }
                    sh "echo funciona"
                    sh 'export PATH=$PATH:/usr/local/maven/bin:/usr/local/openjdk-8/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' + "mvn ${mavenCommands}"
                }
            }
        }
    }
}

def testRunImage(String docker_ip_credential, docker_server_credential) {
    withCredentials([usernameColonPassword(credentialsId: docker_ip_credential, variable: 'IP_PORT')]) {
        withCredentials([dockerCert(credentialsId: docker_server_credential, variable: 'DOCKER_CERT')]) {
            docker.withServer(IP_PORT, 'Docker_Certs') {
                docker.image('alpine:3.19.1').withRun('-e "TEST_TEST_=my-secret-pw"') { c ->
                    sh 'echo one'
                    sh 'echo two'
                }
            }
        }
    }
}

