import org.apache.commons.lang.StringEscapeUtils

String repo_url
String projectName
String gitlabServerUrl

// checkouts vars
Boolean isRegenerate
String localCheckout
String toCheckout

Map configFileYml

// build vars
String buildArgs
String buildVersion
String buildTool
String buildToolVersion
String buildCommands

// gradle or maven
Boolean mavenProject = false
Boolean gradleProject = false

Map CONFIG = [
    CHECKOUT_FOLDER: "repo",
    AGENT: "jdk21",
    CONFIG_PATH_YML: "Pipelines/Generic/config.yml"
]

@Library('release-pipeline@v8-fix') _
pipeline {
    agent { label CONFIG["AGENT"] }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        quietPeriod(10)
    }
    stages {
        stage('Setup parameters . . .') {
            steps {
                script {
                    checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                        branches: [[name: "*/master"]], doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true],
                        [$class: 'LocalBranch', localBranch: "master"],
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: 'repository']],
                        gitTool: 'jgit', submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                        url: 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/release-automation-pipelines.git']]]

                    dir ('repository') { 
                        def params = load env.PATH_DEFINED_PARAMETERS
                        properties([parameters(params)])
                    }
                }
            }
        }
        stage('Read and Merge Configuration . . .') {
            steps {
                script {
                    // set base config.yml
                    configFileYml = readYaml file: CONFIG["CONFIG_PATH_YML"]

                    // set override config.yml if exists
                    if (env.PATH_OVERRIDE_CONFIG != null) {
                        Map overrideConfig = readYaml file: "${env.PATH_OVERRIDE_CONFIG}"
                        configFileYml = mergeConfigs(configFileYml, overrideConfig)
                    }

                    Map saneDefaults = [
                        DOCKER_IP_CREDENTIAL: configFileYml.jenkins.credentials.docker_ip_credential ?: "docker_slave_ip_port",
                        DOCKER_SERVER_CREDENTIAL: configFileYml.jenkins.credentials.docker_server_credential ?: "Docker_Certs",
                        NEXUS_REPOSITORY: configFileYml.nexus.credentials.repository ?: "Ghiggia-60-dias",
                        NEXUS_GROUP_ID: configFileYml.nexus.credentials.group_id ?: "uy.com.geocom.alkosto",
                        NEXUS_CREDENTIAL_ID: configFileYml.nexus.credentials.credential_id ?: "nexus-deploy",
                        GIT_CREDENTIAL_ID: configFileYml.jenkins.credentials.git_credential_id ?: "15940393-32ad-416e-ad80-b8ea71536641",
                        DOCKER_IMAGE: configFileYml.docker.image ?: "nexus.geocom.com.uy/scm-docker-images/openjdk:mvn2.2.1-jdk8",
                        DOCKER_ARGS: configFileYml.docker.args.join(' ') ?: "--add-host 'gitlab.geocom.com.uy:172.24.18.194'",
                        IMAGE_WORKDIR: configFileYml.docker.workdir ?: "/home/jenkins",
                        VOLUMES: configFileYml.docker.volumes.join(' -v ') ?: "/slave/m2/:/root/.m2/ -v /slave/.tmp_builds/tmp/:/project",
                        MAVEN_CONFIG_ID: configFileYml.jenkins.files.maven.config_file_id.toString() ?: "mvn_local_docker_release",
                        MAVEN_TARGET_DIR: configFileYml.jenkins.files.maven.target_location.toString() ?: "/home/jenkins/.m2/settings.xml",
                        GRADLE_CONFIG_ID: configFileYml.jenkins.files.gradle.config_file_id.toString() ?: "65be0b0c-afab-4f50-aad4-dcfa4ac63915",
                        GRADLE_TARGET_DIR: configFileYml.jenkins.files.gradle.target_location.toString() ?: "/home/jenkins/.m2/gradle.properties"
                    ]

                    CONFIG = CONFIG << saneDefaults

                    echo "Configuración final: ${CONFIG}"
                }
            }
        }
        stage('Validate parameters . . .') {
            steps {
                script {
                    // set required parameters
                    List<String> parametersValidation = [params.tarea, params.projectPathWithNamespace, params.branch]

                    // call function
                    validateRequiredParams(parametersValidation)
                }
            }
        }
        stage('Set vars . . .') {
            steps {
                script {
                    repo_url = StringEscapeUtils.unescapeHtml(params.repo_url)
                    projectName = params.projectPathWithNamespace.split("/").last()
                    gitlabServerUrl = repo_url.contains('git@') ? repo_url.split('@')[1].toString().split(':')[0] : repo_url.split('/')[2]

                    // checkouts vars
                    isRegenerate = params.tarea == "regenerate_version"
                    localCheckout = isRegenerate ? params.version : params.branch
                    toCheckout = (isRegenerate ? "refs/tags/${projectName}-${params.version}" : "*/${params.branch}")  

                    // build vars
                    buildArgs = params.build_args ?: ""
                    buildToolVersion = params.buildToolVersion
                    buildCommands = getBuildCommands(params)

                    // Define volumes for SSH keys and build artifacts
                    env.TMP_BUILD_VOLUME = "${CONFIG["IMAGE_WORKDIR"]}/${UUID.randomUUID()}"
                    println("\n TMP_BUILD_VOLUME IS: ${env.TMP_BUILD_VOLUME} \n")
                    env.TMP_JENKINS_VOLUME = env.TMP_BUILD_VOLUME.replaceAll("${CONFIG["IMAGE_WORKDIR"]}/", "/tmp/builds/")
                    println("\n TMP_JENKINS_VOLUME IS: ${env.TMP_JENKINS_VOLUME} \n")
                }
            }
        }
        stage('Build . . .') {
            steps {
                script {
                    dir(CONFIG["CHECKOUT_FOLDER"]) {

                        println("ESTOY EN EJECUCION DE EL BUILD")
                        println("CHECKOUT FOLDER ES ${CONFIG["CHECKOUT_FOLDER"]}")
                        
                        String sshVolume = "/tmp/${UUID.randomUUID()}"

                        Closure dockerRun = {   // Start Docker container with specific configurations
                            
                            // Securely handle SSH keys within the pipeline
                            withCredentials([sshUserPrivateKey(credentialsId: CONFIG["GIT_CREDENTIAL_ID"], keyFileVariable: 'sshKey')]) {
                                sh "mkdir -p ${sshVolume}" // Create directory (pipeline) for SSH keys
                                // Write the private SSH key to a temporary volume
                                writeFile file: "${sshVolume}/id_rsa", text: readFile("${sshKey}")
                            }

                            dockerArgs = [
                                "-u 0",
                                "-v ${CONFIG["VOLUMES"]}",
                                "${CONFIG["DOCKER_ARGS"]}"
                            ].join(" ")

                            // Execute the container with the prepared arguments
                            return runDockerContainer(CONFIG["DOCKER_IMAGE"], dockerArgs)
                        }

                        Closure dockerExec = { containerId -> // exec in docker
                            // Define a closure to execute shell commands inside the Docker container
                            Closure shDocker = { command -> sh "docker exec ${containerId} sh -c '${command}'" }

                            // Setup SSH configuration within the container
                            shDocker "mkdir -p /root/.ssh/ ${env.TMP_BUILD_VOLUME} && chown -R 1000:1000 ${env.TMP_BUILD_VOLUME}"
                            sh "docker cp ${sshVolume}/id_rsa ${containerId}:/root/.ssh/id_rsa" // Copy the SSH key into the container
                            
                            shDocker "chmod 600 /root/.ssh/id_rsa && ssh-keyscan -H gitlab.geocom.com.uy >> /root/.ssh/known_hosts &> /dev/null"

                            // Configure Git global settings
                            shDocker """
                                git config --global user.email geoscm@geocom.com.uy
                                git config --global user.name 'jenkins'
                            """

                            // Clone the project repository and set directory permissions
                            shDocker """
                                git clone ${repo_url} ${env.TMP_BUILD_VOLUME}/${projectName}
                                cd ${env.TMP_BUILD_VOLUME}/${projectName} && git checkout ${localCheckout}
                                chown -R 1000:1000 ${env.TMP_BUILD_VOLUME}
                                git config --global --add safe.directory ${env.TMP_BUILD_VOLUME}/${projectName}
                            """

                            // Read Maven project version from the POM file
                            dir("${env.TMP_JENKINS_VOLUME}/${projectName}") {

                                if (fileExists('pom.xml') && !fileExists('build.gradle')) {
                                    mavenProject = true
                                    echo 'Proyecto Maven'
                                } else if (fileExists('build.gradle') && !fileExists('pom.xml')){
                                    gradleProject = true
                                    echo 'Project Gradle'
                                }

                                if (mavenProject) {
                                    buildVersion = readMavenPom(file: 'pom.xml').getVersion().replaceAll("-SNAPSHOT", "")
                                    buildTool = params.wrapper ? "./mvnw" : "mvn"
                                }
                                if (gradleProject) {
                                    def properties = readProperties file: 'gradle.properties'
                                    buildVersion = properties['version'].replaceAll('-SNAPSHOT','')
                                    buildTool = params.wrapper ? "./gradlew" : "gradle"
                                }

                                configFileProvider([configFile(fileId: mavenProject ? CONFIG["MAVEN_CONFIG_ID"] : CONFIG["GRADLE_CONFIG_ID"],
                                    targetLocation: mavenProject ? CONFIG["MAVEN_TARGET_DIR"] : CONFIG["GRADLE_TARGET_DIR"])] ) {
                                    if (gradleProject) {
                                        shDocker "mkdir -p /root/.gradle"
                                        sh "docker cp ${env.HOME}/.m2/gradle.properties ${containerId}:/root/.gradle/gradle.properties" // Copy the SSH key into the container
                                    }
                                }
                            }

                            // Execute Maven commands within the project directory and adjust directory permissions post-build
                            shDocker """
                                cd ${env.TMP_BUILD_VOLUME}/${projectName} && ${buildTool} ${buildCommands} ${buildArgs}
                                chown -R 1000:1000 ${env.TMP_BUILD_VOLUME}
                            """
                        }
                        // exec closures
                        withDockerEnvironment(CONFIG, dockerRun, dockerExec, env.TMP_BUILD_VOLUME)
                        
                        // set job display name
                        currentBuild.displayName = "#${BUILD_NUMBER}-${projectName}-${buildVersion}"               
                    }
                }
            }
        }
        stage('Publish to Nexus . . .') {
            when {
                expression {
                    return params.tarea != "build"
                }
            }
            steps {
                script {
                    dir("${env.TMP_JENKINS_VOLUME}/${projectName}") {
                        geoNexusUpload 'zip', CONFIG["NEXUS_REPOSITORY"], CONFIG["NEXUS_GROUP_ID"]
                    }
                }
            }
        }
        stage('Cleaning . . .') {
            steps {
                script {
                    sh "rm -rf ${env.TMP_JENKINS_VOLUME}"
                }
            }
        }
        stage('Install . . .') {
            when {
                expression { 
                    //return params.INSTALL && params.tarea != "build"
                    return false
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
                        string(name: 'version', value: buildVersion),
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
            sh "rm -rf ${env.TMP_JENKINS_VOLUME}"
        }
    }
}

def getBuildCommands(params) {
    switch(params.tarea) {
        case 'release':
            return params.release_param
        case 'regenerate_version':
            return params.build_param
        default:
            return params.build_param
    }
}

def withDockerEnvironment(config, Closure dockerRunConfig, Closure body, String tmpBuildVolme) {
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
                        stopAndRemoveDockerContainer(containerId, tmpBuildVolme)
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

def stopAndRemoveDockerContainer(String containerId, String tmpBuildVolme) {
    def shDocker = { command -> sh "docker exec ${containerId} sh -c '${command}'" }
    shDocker "chown -R 1000:1000 ${tmpBuildVolme}"
    sh "docker stop ${containerId}; docker rm ${containerId}"
}

// function to combine yml base configurations with override configurations if they exist
def mergeConfigs(baseConfig, overrideConfig) {
    overrideConfig.each { key, value ->
        def path = key.split("\\.") as List
        def lastKey = path.pop()

        def current = baseConfig
        path.each { part ->
            if (!current.containsKey(part)) {
                current[part] = [:]
            } else if (!(current[part] instanceof Map)) {
                current[part] = [:]
            }
            current = current[part]
        }
        
        if (current[lastKey] instanceof Map && value instanceof Map) {
            current[lastKey] = mergeConfigs(current[lastKey], value)
        } else {
            current[lastKey] = value
        }
    }
    return baseConfig
}

def validateRequiredParams(List<String> parametersValidation) {
    parametersValidation.each { p ->
        if (p == null || p.isEmpty()) {
            error("${p} is empty or null")
        }
    }
}

