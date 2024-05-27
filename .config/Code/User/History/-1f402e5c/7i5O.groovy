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
        choice(name: 'ambiente', choices: ['QA','DEV','ARGENTINA', 'BETA-ORIG', 'CHILE'], description: 'Pick something')
        choice(name: 'tags', choices: ['java', 'kafka', 'keepalived'], description: 'Pick something')
    }
    stages {
        stage('Kafka Topics') {
            steps {
                script {
                    def BRANCH = "scm-test"
                    sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {
                                                
                        if(params.accion!="delete") {
                            try{
                                sh "mkdir plugins"
                                sh "curl -s -o plugins/mongodb-kafka-connect-mongodb-1.10.0.zip https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-kafka-connect-mongodb-1.10.0.zip"
                                sh "curl -s -o plugins/avro-1.11.0.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/avro-1.11.0.jar"
                                sh "curl -s -o plugins/mongo-kafka-connect-1.10.0-all.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongo-kafka-connect-1.10.0-all.jar"
                                sh "curl -s -o plugins/mongodb-driver-sync-4.9.0.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-driver-sync-4.9.0.jar"
                                sh "ls -la plugins"
                                sh "pwd"
                            }catch (e){
                                println "Error fetching jars"
                            }
                        }

                        sshagent(['15940393-32ad-416e-ad80-b8ea71536641']) {
                            configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: "ansible.cfg")]) {
                                sh """
                                    ansible-galaxy install -r ansible/requirements.yml && \
                                    ansible-playbook ansible/playbooks/provisioning_kafka/main.yml \
                                    -i client/Environments/${params.ambiente}/inventory/${params.ambiente} \
                                    -e '@ansible/ansible.vars.yml' \
                                    -e 'repo_home=${pwd()}'\
                                    -e 'ambiente=${params.ambiente}' \
                                    -t '${params.accion}'
                                """
                            }
                        }
                    }
                }    
            }
        }
    }
}