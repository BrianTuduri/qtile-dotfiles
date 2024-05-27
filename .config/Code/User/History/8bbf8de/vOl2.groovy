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
        choice(name: 'accion', choices: ['apply-topic', 'apply-connect', 'add-connectors', 'delete-topics' ,'delete-connectors'], description: 'Pick something')
    }
    stages {
        stage('Kafka Topics') {
            steps {
                script {
                    def BRANCH = "scm-test"
                    sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {

                        git credentialsId: "15940393-32ad-416e-ad80-b8ea71536641", url: "git@gitlab.geocom.com.uy:uy-com-geocom-scm/express-devops.git", branch: BRANCH
                        
                        sh """
                            git config pull.rebase false
                            git submodule init && git submodule update --remote --merge
                            cd Environments 
                            git checkout ${BRANCH}
                            git pull
                        """
                        if(params.accion!="delete") {
                            try{
                                sh "mkdir utils"
                                sh "curl -s -o utils/mongodb-kafka-connect-mongodb-1.10.0.zip https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-kafka-connect-mongodb-1.10.0.zip"
                                sh "curl -s -o utils/avro-1.11.0.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/avro-1.11.0.jar"
                                sh "curl -s -o utils/mongo-kafka-connect-1.10.0-all.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongo-kafka-connect-1.10.0-all.jar"
                                sh "curl -s -o utils/mongodb-driver-sync-4.9.0.jar https://nexus.geocom.com.uy/repository/Ghiggia/tools/swbase/64bits/kafka/mongodb-driver-sync-4.9.0.jar"
                                sh "ls -la utils"
                                sh "pwd"
                            }catch (e){
                                println "Error fetching jars"
                            }
                        }
                        
                        sshagent(['15940393-32ad-416e-ad80-b8ea71536641']) {
                            configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: "ansible.cfg")]) {
                                sh """
                                    ansible-galaxy install -r Ansible/requirements.yml && \
                                    ansible-playbook ansible/playbooks/provisioning_kafka/main.yml \
                                    -i inventory/${params.ambiente} \
                                    -e '@Ansible/playbooks/kafka/defaults/main.yml' \
                                    -e 'repo_home=${pwd()}'\
                                    -e 'ambiente=${params.ambiente}' \
                                    -t '${params.accion}'
                                """
                                // ansible/playbooks/provisioning_kafka/main.yml
                            }
                        }
                    }
                }    
            }
        }
    }
}