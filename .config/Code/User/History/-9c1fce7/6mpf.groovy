// plugins to Kafka
def nexusPluginsUrls = [
    // "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-kafka-connect-mongodb-1.10.0.zip",
    // "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/avro-1.11.0.jar",
    // "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongo-kafka-connect-1.10.0-all.jar",    
    // "https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-driver-sync-4.9.0.jar"
] //.join(',')
def kafka_deploy_branch = "master"
def gitUrl = [:]
def sshCreds = ""

pipeline {
    agent { label 'ansible-ssh' }
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
        choice(name: 'environmentsBranch', choices: ['master'], description: 'Select environments branch')
        choice(name: 'ambiente', choices: ['CERT'], description: 'Select enviroment')
        choice(name: 'accion', choices: ['apply-topic', 'apply-connect', 'add-connectors', 'delete-topics' ,'delete-connectors'], description: 'Pick something')
        string(name: 'FILE_URLS', defaultValue: nexusPluginsUrls,description: 'URLs of files to download, separated by coma')
        choice(name: 'instanceEnv', choices: ['GAMA'], description: 'Pick something')
    }
    stages {
        stage('Init submodule . . .') {
            steps {
                script {
                    stage("Set settings . . .") {
                        steps {
                            script {
                            switch(params.instanceEnv) {
                                case "GAMA":
                                    gitUrl = [
                                        "geoscm-ansible-collection" : "git@gitlab-gama.geocom.com.uy:scm/geoscm-ansible-collection.git",
                                        "kubernetes-ansible-collection" : "git@gitlab-gama.geocom.com.uy:scm/DevOPS/kubernetes-ansible-collection.git"
                                        "kafka-deploy-repo" : "git@gitlab.geocom.com.uy:uy-com-geocom-scm/kafka-deploy-gama.git"
                                        "environments-configuration" : "git@gitlab.geocom.com.uy:uy-com-geocom-scm/gama-environments-configuration.git"
                                    ]
                                    sshCreds = "15940393-32ad-416e-ad80-b8ea71536641"
                                break
                                default:
                                    gitUrl = [
                                        "geoscm-ansible-collection" : "git@gitlab.geocom.com.uy:scm/geoscm-ansible-collection.git",
                                        "kubernetes-ansible-collection" : "git@gitlab.geocom.com.uy:scm/DevOPS/kubernetes-ansible-collection.git"
                                    ]
                                    sshCreds = "15940393-32ad-416e-ad80-b8ea71536641"
                                break
                            }
                            def requirmentsYml = """
                            collections:
                            - name: ${gitUrl['geoscm-ansible-collection']}
                                type: git
                                version: master
                            - name: ${gitUrl['kubernetes-ansible-collection']}
                                type: git
                                version: master
                            """
                            writeFile(file: 'ansible/requirements.yml', text: "${requirmentsYml}")
                            writeFile(file: '.gitmodules', text: """
                            [submodule "Environments"]
                                path = Environments
                                url = ${gitUrl['environments-configuration']}
                            """)
                            }
                        }
                    }

                    sshagent([sshCreds]) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${kafka_deploy_branch}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [
                                [$class: 'RelativeTargetDirectory', relativeTargetDir: 'client']
                            ],
                            gitTool: 'GIT Runner',
                            submoduleCfg: [],
                            userRemoteConfigs: [[
                                credentialsId: sshCreds,
                                url: gitUrl['geoscm-ansible-collection']
                            ]]
                        ])
                        dir('client') {
                            sh "git config pull.rebase false"
                            sh "git submodule init"
                            sh "git submodule update --remote --merge"
                            dir('Environments') {
                                sh """
                                    git checkout ${params.environmentsBranch}
                                    git pull
                                """
                            }
                        } 
                    }
                }
            }
        }
        stage('Get plugins . . .') {
            when { expression { return params.accion != "delete" } }
            steps {
                script {
                    dir('client') {
                        try {
                            sh "mkdir -p plugins"
                            def urls = params.FILE_URLS.trim().split(",")
                            urls.each { url ->
                                println("URL IS: ${url}")
                                def archivo = url.split("/").last()
                                println("curl -s -o plugins/${archivo} ${url}")
                                sh("curl -s -o plugins/${archivo} ${url}")
                            }
                        } catch (Exception e) {
                            error("Error fetching jars: ${e.message}")
                        }
                    }
                }
            }
        }
        
        stage('Ansible provisioning Kafka . . .') {
            steps {
                script {
                    dir('client') {
                        // cy5ymCych3DLs29tpAnCC2qF0ze5RdPT
                        sshagent([sshCreds]) { sh "ansible-galaxy install -r ansible/requirements.yml" }
                        configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: "ansible.cfg")]) {
                            ansiblePlaybook(
                                installation: 'Ansible',
                                playbook: 'ansible/playbooks/provisioning_kafka/main.yml',
                                inventory: "ansible/inventory.ini",
                                credentialsId: sshCreds,
                                extraVars: [
                                    repo_home: "${pwd()}",
                                    ambiente: "${params.ambiente}"
                                ],
                                tags: "${params.accion}",
                                extras: "-e '@ansible/ansible.vars.yml' -e '@Environments/${params.ambiente}/Kafka/kafka-connect.yml'",
                                colorized: true
                            )
                        }
                    }
                }
            }
        }
    }
}