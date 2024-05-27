def gradleVersion
def extension
def gradleParams
def proyecto = params.proyecto.split('/')[-1]
def path_build = proyecto == "geohub" ? "backend/" : ""

pipeline {
    agent { label "jdk21_jdk8" }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        quietPeriod(10)
    }
    tools {
        jdk 'JDK_8'
    }
    stages {
        stage('Clone'){
            steps {
                script {
                    def isRegenerate = params.tarea == "regenerate_version"
                    def BRANCH = "master"
                    def toCheckout = (isRegenerate ? "refs/tags/${proyecto}-${params.version}" : "*/${params.branch}")

                    //Checkout del proyecto seleccionado
                    checkout([$class                           : 'GitSCM',
                            branches                         : [[name: toCheckout]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'repo'],
                                                                [$class: 'LocalBranch', localBranch: (isRegenerate ? params.version : params.branch)]],
                            gitTool                          : 'jgit',
                            submoduleCfg                     : [],
                            userRemoteConfigs                : [[credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                                                                url          : params.service_repo_url]]])

                    //Checkout de este mismo proyecto, esto se realiza para poder leer el archivo servicios.yaml
                    checkout([$class                           : 'GitSCM',
                            branches                         : [[name: "*/fix-brian"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'SCM'],
                                                                [$class: 'LocalBranch', localBranch: BRANCH]],
                            gitTool                          : 'jgit',
                            submoduleCfg                     : [],
                            userRemoteConfigs                : [[credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                                                                url          : 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/geopos2-alkosto-scm.git']]])


                }
            }
        }
        //Lee los parametros del archivo servicios.yaml donde se indican parametros necesarios
        stage('Read yaml'){
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
        stage('Release'){
            steps {
                configFileProvider([configFile(fileId: '65be0b0c-afab-4f50-aad4-dcfa4ac63915', targetLocation: '~/.gradle/gradle.properties')]) {
                    script {
                        dir('repo'){
                            sh """      
                                ls -la /root/.ssh
                                git config --global user.name "Jenkins"
                                git config --global user.email "Jenkins@Geocom.com"
                            """

                            //Hash map coon los servicios que necesitan NodeJS y ademas en el valor esta la version del NodeJS
                            def projects_NodeJS = [ 'geohub' : "NodeJS 8.11.3", 'digi-services' : "NodeJs 6.3.0", 'geopos-audit-log-service' : "NodeJs 6.3.0", 'alkosto-aws-mediator' : "NodeJs 6.3.0"]
                            def projectWithNode = projects_NodeJS.containsKey(proyecto)

                            //Se realiza la tarea dependiendo de las opciones que se elijieron en Jenkins
                            sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                                
                                def properties = readProperties file: 'gradle.properties'
                                gradleVersion = properties['version'].replaceAll('-SNAPSHOT','')
                                println "Version-- [${gradleVersion}]--"
                                switch (params.tarea) {
                                    case "release":
                                        if (projectWithNode){
                                            println projects_NodeJS["${proyecto}"]
                                            nodejs(projects_NodeJS["${proyecto}"]) {
                                                sh """
                                                    ./gradlew check
                                                    ./gradlew assemble
                                                    ./gradlew release ${gradleParams}
                                            """
                                                //./gradlew jib -Djib.to.auth.username="$CI_REGISTRY_USER" -Djib.to.auth.password="$CI_REGISTRY_PASSWORD" -Djib.to.tags="$gradleVersion"
                                            }
                                        }else{
                                            sh """
                                                    ./gradlew check
                                                    ./gradlew assemble
                                                    ./gradlew release ${gradleParams}
                                            """
                                            //./gradlew jib -Djib.to.auth.username="$CI_REGISTRY_USER" -Djib.to.auth.password="$CI_REGISTRY_PASSWORD" -Djib.to.tags="$gradleVersion"
                                        }
                                        break;
                                    case "regenerate_version":
                                        if (projectWithNode){
                                            nodejs(projects_NodeJS["${proyecto}"]) {

                                                sh """
                                                    ./gradlew build
                                                """
                                                //./gradlew jib -Djib.to.auth.username="$CI_REGISTRY_USER" -Djib.to.auth.password="$CI_REGISTRY_PASSWORD" -Djib.to.tags="$gradleVersion"

                                            }
                                        }else{
                                            sh """
                                                    ./gradlew build
                                                """
                                            //./gradlew jib -Djib.to.auth.username="$CI_REGISTRY_USER" -Djib.to.auth.password="$CI_REGISTRY_PASSWORD" -Djib.to.tags="$gradleVersion"
                                        }
                                        break;
                                    default:
                                        if (projectWithNode){
                                            nodejs(projects_NodeJS["${proyecto}"]) {
                                                sh "./gradlew build"
                                            }
                                        }else{
                                            sh "./gradlew build"
                                        }
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
        //Realizacion del zip a subir a Nexus, se podrian agregar los scripts en esta seccion
        stage('zip'){
            steps {
                script{
                    dir("repo/${path_build}build/libs"){
                        if (params.tarea != "build") {
                            sh "mkdir ${proyecto}-${gradleVersion}"
                            sh "cp ${proyecto}-${gradleVersion}.jar ${proyecto}-${gradleVersion}"
                            zip zipFile: "${proyecto}-${gradleVersion}.zip", archive: false, dir: "${proyecto}-${gradleVersion}"
                        }
                    }
                }
            }
        }
        //Subida de archivo a Nexus
        stage('Upload Nexus'){          
            when {
                expression {
                    return params.tarea != "build"
                }
            }
            steps { 
                script {
                    dir('repo'){
                        def file = "${proyecto}-${gradleVersion}.${extension}"
                        dir("${path_build}build/libs") {
                            println "Version-- [${gradleVersion}]--"
                            println "Archivo para deploy Nexus -- [${file}]--"
                            nexusArtifactUploader artifacts: [[artifactId: "${proyecto}", classifier: '', file: file, type: "${extension}"]],
                                    credentialsId: 'nexus-deploy',
                                    groupId: 'uy.com.geocom.alkosto.microservicios',
                                    nexusUrl: 'nexus.geocom.com.uy',
                                    nexusVersion: 'nex1us3',
                                    protocol: 'https',
                                    repository: 'Ghiggia-60-dias',
                                    version: "${gradleVersion}"
                        }
                    }
                }
            }
        }
    }
}
