// plugins to Kafka
def nexusPluginsUrls = [
    "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-kafka-connect-mongodb-1.10.0.zip",
    "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/avro-1.11.0.jar",
    "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongo-kafka-connect-1.10.0-all.jar",    
    "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-driver-sync-4.9.0.jar"
].join(',')

pipeline {
    agent { label 'ansible-2024' }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        disableConcurrentBuilds()
        quietPeriod(10)
    }
    environment{
        GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no"
    }
    parameters {
        choice(name: 'branch', choices: ['master', 'test-brian'], description: 'Select branch')
        choice(name: 'ambiente', choices: ['QA','DEV','ARGENTINA', 'BETA-ORIG', 'CHILE'], description: 'Select enviroment')
        choice(name: 'accion', choices: ['apply-topic', 'apply-connect', 'add-connectors', 'delete-topics' ,'delete-connectors'], description: 'Pick something')
        string(name: 'FILE_URLS', defaultValue: nexusPluginsUrls,description: 'URLs of files to download, separated by coma')
    }
    stages {
        stage('Get plugins . . .') {
            when { expression { return params.accion != "delete" } }
            steps {
                script {
                    // Error fetching jars: No signature of method: java.util.Collections$UnmodifiableMap
                    try {
                        def pluginsDir = new File("${WORKSPACE}/plugins")
                        if (!pluginsDir.exists()) {
                            pluginsDir.mkdirs()
                        }
                        def urls = params.FILE_URLS.trim().split(",")
                        urls.each { url ->
                            def archivo = url.split("/").last()
                            sh "curl -s -o ${pluginsDir}/${archivo} ${url}"
                        }
                    } catch (Exception e) {
                        println "Error fetching jars: ${e.message}"
                    }
                }
            }
        }
        stage('Init submodule . . .') {
            steps {
                script {
                    sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.branch}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [
                                [$class: 'RelativeTargetDirectory', relativeTargetDir: 'client']
                            ],
                            gitTool: 'GIT Runner',
                            submoduleCfg: [],
                            userRemoteConfigs: [[
                                credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                                url: 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/kafka-deploy-oxxo.git'
                            ]]
                        ])
                        dir('client') {
                            sh "git config pull.rebase false"
                            sh "git submodule init"
                            sh "git submodule update --remote --merge"
                            dir('Environments') {
                                sh """
                                    git checkout ${params.branch}
                                    git pull
                                """
                            }
                        } 
                    }
                }
            }
        }
        stage('Ansible provisioning Kafka . . .') {
            steps {
                script {
                    dir('ansible') {
                        sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) { sh "ansible-galaxy install -r requirements.yml" }
                        ansiblePlaybook(
                            installation: 'Ansible',
                            playbook: 'playbooks/provisioning_kafka/main.yml',
                            inventory: "client/Environments/${params.ambiente}/inventory/${params.ambiente}",
                            credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                            extraVars: [
                                repo_home: "${pwd()}",
                                ambiente: "${params.ambiente}"
                            ],
                            tags: "${params.accion}",
                            extras: "-e '@ansible.vars.yml'"
                        )
                    }
                }
            }
        }
    }
}