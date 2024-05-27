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
        choice(name: 'branch', choices: ['master','scm-test'], description: 'Select branch')
        choice(name: 'kafka_version', choices: ['3.6.1'], description: 'Version de Kafka')
        choice(name: 'kafka_scala_version', choices: ['2.13'], description: 'Version de Scala')
        choice(name: 'kafka_openjdk_version', choices: ['21'], description: 'Version de openjdk')
        choice(name: 'ambiente', choices: ['QA','DEV','ARGENTINA', 'BETA-ORIG', 'CHILE'], description: 'Pick something')
        extendedChoice(
            name: 'tags',
            type: 'PT_CHECKBOX',
            description: 'Selecciona el tag a ejecutar',
            multiSelectDelimiter: ',',
            value: 'java,kafka,keepalived',
            defaultValue: 'false,false,false',
            visibleItemCount: 3,
            quoteValue: false
        )
    }
    stages {
        stage('Ansible') {
            steps {
                script {
                    println(params.tags)
                    def selectedTags = params.tags.tokenize(',')
                    def ansible_tags = selectedTags.join(',')
                    def BRANCH = "scm-test"
                    dir('ansible') {
                        sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                            sshagent(['15940393-32ad-416e-ad80-b8ea71536641']) {
                                configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: "ansible.cfg")]) {
                                    sh "ansible-galaxy install -r requirements.yml"
                                    ansiblePlaybook(
                                        installation: 'Ansible',
                                        playbook: 'playbooks/install_kafka/main.yml',
                                        inventory: "inventory.ini",
                                        credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                                        extraVars: [
                                            repo_home: "${pwd()}",
                                            ambiente: "${params.ambiente}",
                                            ansible_user: "root"
                                        ],
                                        tags: "${ansible_tags}",
                                        extras: "-e '@ansible.vars.yml'"
                                    )
                                }
                            }
                        }
                    }
                }    
            }
        }
    }
}