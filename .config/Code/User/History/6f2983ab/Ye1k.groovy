//////
def gradleVersion
def extension
def gradleParams
def proyecto = params.proyecto.split('/')[-1]
def path_build = proyecto == "geohub" ? "backend/" : ""
//Hash map coon los servicios que necesitan NodeJS y ademas en el valor esta la version del NodeJS
def projects_NodeJS = [ 'geohub' : "NodeJS 8.11.3", 'digi-services' : "NodeJs 6.3.0", 'geopos-audit-log-service' : "NodeJs 6.3.0", 'alkosto-aws-mediator' : "NodeJs 6.3.0"]
def projectWithNode = projects_NodeJS.containsKey(proyecto)
//////


def isRegenerate = params.tarea == "regenerate_version"
def localCheckout = isRegenerate ? params.version : params.branch
def toCheckout = (isRegenerate ? "refs/tags/${proyecto}-${params.version}" : "*/${params.branch}")
def URL_REPO = "git@gitlab.geocom.com.uy:uy-com-geocom-alkosto/geopos2-alkosto.git"
def baseRepoUrl = URL_REPO.replaceFirst(/\.git$/, "")
def projectName = baseRepoUrl.tokenize('/').last()
def gradleCommands = getGradleCommands(params.tarea)
def tmpBuildVolume
def tmpJenkinsVolume
def mavenArgs = [""].join(" ")

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
    SCM_BRANCH: "fix-brian"
]

pipeline {
    agent { label "${CONFIG["LABEL"]}" }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        quietPeriod(10)
    }
    environment {
        DOCKER_IMAGE = "nexus.geocom.com.uy/scm-docker-images/gradle-4.10.3-all-jdk8"
        IMAGE_WORKDIR = "/project"
        MVN_VOLUME = "/slave/m2/:/root/.m2/"
    }
    stages {
        stage('Checkout . . .') {
            steps {
                script {
                    checkout([$class                           : 'GitSCM',
                            branches                         : [[name: "*/${CONFIG["BRANCH"]}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'SCM'],
                                                                [$class: 'LocalBranch', localBranch: CONFIG["BRANCH"]]],
                            gitTool                          : 'jgit',
                            submoduleCfg                     : [],
                            userRemoteConfigs                : [[credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                                                                url          : 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/geopos2-alkosto-scm.git']]])
                }
            }
        }

        stage('Read yaml . . .'){
            steps {
                script{
                    dir('SCM'){
                        def varYml = readYaml file : 'Jobs/MicroServices/servicios.yaml'
                        extension = varYml["${proyecto}"]["extension"]
                        gradleParams = varYml["${proyecto}"]["gradleParams"]

                        println "Extension--- ${extension}"
                        println "Gradle Params ${gradleParams}"
                    }
                }
            }
        }

        stage('Ejecucion Gradle . . .') {
            steps {
                script {
                    dir(CONFIG["CHECKOUT_FOLDER"]) {
                        def CURRENT_BUILD_VOLUME = "/slave/.tmp_builds/:${IMAGE_WORKDIR}/"
                        configFileProvider([configFile(fileId: '65be0b0c-afab-4f50-aad4-dcfa4ac63915', targetLocation: "${env.HOME}/.gradle/gradle.properties")]) {
                            def sshVolume
                            withDockerEnvironment(CONFIG, 
                                {   // Start Docker container with specific configurations
                                    // Define volumes for SSH keys and build artifacts
                                    sshVolume = "/tmp/${UUID.randomUUID()}"
                                    tmpBuildVolume = "${IMAGE_WORKDIR}/tmp/${UUID.randomUUID()}"
                                    tmpJenkinsVolume = tmpBuildVolume.replaceAll("${IMAGE_WORKDIR}/", "/tmp/builds/")
                                    
                                    // Securely handle SSH keys within the pipeline
                                    withCredentials([sshUserPrivateKey(credentialsId: CONFIG["GIT_CREDENTIAL_ID"], keyFileVariable: 'sshKey')]) {
                                        sh "mkdir -p ${sshVolume}" // Create directory (pipeline) for SSH keys
                                        // Write the private SSH key to a temporary volume
                                        writeFile file: "${sshVolume}/id_rsa", text: readFile("${sshKey}")
                                    }

                                    // Prepare Docker arguments for running the container
                                    // -u 0: Run as root inside the container
                                    // -v ${MVN_VOLUME}: Mount Maven volume for caching dependencies
                                    // -v ${CURRENT_BUILD_VOLUME}: Mount the build volume for the project
                                    // --add-host: Manually add an entry to the container's /etc/hosts
                                    dockerArgs = [
                                        "-u 0",
                                        "-v ${MVN_VOLUME}",
                                        "-v ${CURRENT_BUILD_VOLUME}",
                                        "--add-host 'gitlab.geocom.com.uy:172.24.18.194'"
                                    ].join(" ")

                                    // Execute the container with the prepared arguments
                                    return runDockerContainer(DOCKER_IMAGE, dockerArgs)
                                },
                                { containerId -> // exec in docker
                                    // Define a closure to execute shell commands inside the Docker container
                                    def shDocker = { command -> sh "docker exec ${containerId} sh -c '${command}'" }

                                    // Setup SSH configuration within the container
                                    shDocker "mkdir -p /root/.ssh/ ${tmpBuildVolume}"
                                    sh "docker cp ${sshVolume}/id_rsa ${containerId}:/root/.ssh/id_rsa" // Copy the SSH key into the container
                                    shDocker "chmod 600 /root/.ssh/id_rsa && ssh-keyscan -H gitlab.geocom.com.uy >> /root/.ssh/known_hosts &> /dev/null"

                                    // Configure Git global settings
                                    shDocker """
                                        git config --global user.email geoscm@geocom.com.uy
                                        git config --global user.name 'jenkins'
                                    """

                                    // Clone the project repository and set directory permissions
                                    shDocker """
                                        git clone ${URL_REPO} ${tmpBuildVolume}/${projectName}
                                        cd ${tmpBuildVolume}/${projectName} && git checkout ${localCheckout}
                                        chown -R 1000:1000 ${tmpBuildVolume}
                                        git config --global --add safe.directory ${tmpBuildVolume}/${projectName}
                                    """

                                    // Read Maven project version from the POM file
                                    dir("${tmpJenkinsVolume}/${projectName}") {
                                        def properties = readProperties file: 'gradle.properties'
                                        gradleVersion = properties['version'].replaceAll('-SNAPSHOT','')
                                    }

                                    // Execute Maven commands within the project directory and adjust directory permissions post-build
                                    shDocker """
                                        cd ${tmpBuildVolume}/${projectName} && mvn ${mavenCommands} ${mavenArgs}
                                        chown -R 1000:1000 ${tmpBuildVolume}
                                    """
                                }
                            )
                        }
                    }
                }
            }
        }

        stage('Publicando zip en Nexus . . .') {
            when {
                expression {
                    return params.tarea != "build"
                }
            }
            steps {
                script {
                    def zipName = "geopos2-alkosto-${pomVersion}-Package.zip"
                    dir("${tmpJenkinsVolume}/${projectName}/target") {
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

        stage('Cleaning . . .') {
            steps {
                script {
                    sh "rm -rf ${tmpJenkinsVolume}"
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
    post {
        always {
            sh "rm -rf ${tmpJenkinsVolume}"
        }
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
def executeTask(String project, String task, String gradleParams, Closure shDockerClosure) {
    def projectWithNode = projects_NodeJS.containsKey(project)
    
    def shDocker = shDockerClosure
    
    switch (task) {
        case "release":
            if (projectWithNode) {
                shDocker("nodejs ${projects_NodeJS[project]} && ./gradlew check && ./gradlew assemble && ./gradlew release ${gradleParams}")
            } else {
                shDocker("./gradlew check && ./gradlew assemble && ./gradlew release ${gradleParams}")
            }
            break
        case "regenerate_version":
            if (projectWithNode) {
                shDocker("nodejs ${projects_NodeJS[project]} && ./gradlew build")
            } else {
                shDocker("./gradlew build")
            }
            break
        default:
            if (projectWithNode) {
                shDocker("nodejs ${projects_NodeJS[proyecto]} && ./gradlew build")
            } else {
                shDocker("./gradlew build")
            }
            break
    }
}
