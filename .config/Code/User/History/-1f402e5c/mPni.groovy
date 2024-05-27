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
        choice(name: 'tags', choices: ['java', 'kafka', 'keepalived'], description: 'Pick something')
    }
    stages {
        stage('Kafka Topics') {
            steps {
                script {
                    def BRANCH = "scm-test"
                    sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) {

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