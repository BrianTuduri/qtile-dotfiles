pipeline {
    agent { label "ansible-2024" + params.JENKINS_INSTANCE }
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(
                artifactDaysToKeepStr: '',
                artifactNumToKeepStr: '',
                daysToKeepStr: '',
                numToKeepStr: '5'
        )
        disableConcurrentBuilds()
        quietPeriod(5)
    }
    stages {
        stage('Run Playbook') {
            steps {
                script {
                    LinkedHashMap<String, String> pVars = [
                            ansible_user        : 'root',
                            systemd_service_name: 'oms-compose.service',
                            deploy_path         : '/home/geocom/oms',
                            client              : params.Ambiente.split('-')[0],
                            env                 : params.Ambiente.split('-')[1],
                            playbookBranch      : 'newEnv',
                            registry_user       : "geocloudvandamme",
                            JENKINS_INSTANCE    : params.JENKINS_INSTANCE
                    ]

                    withCredentials([usernamePassword(credentialsId: 'GITLAB_READ_WRITE', passwordVariable: 'GITLABTOKEN', usernameVariable: 'GITLABTOKEN_USER')]) {
                        sh """
                            git config --global url.\"https://oauth2:$GITLABTOKEN@gitlab.geocom.com.uy\".insteadOf https://gitlab.geocom.com.uy
                            git clone --depth 1 --bare https://gitlab.geocom.com.uy/uy-com-geocom-scm/devops/oms-rancher-deploy.git -b ${pVars['playbookBranch']}
                        """
                        pVars['env_directory'] = "${pwd()}/Environments/AmbientesDockerCompose/${params.Ambiente}"
                        pVars['env_rabbit'] = "${pwd()}/Environments/"
                    }

                    withCredentials([usernamePassword(credentialsId: 'a8bd70c2-566d-475c-9868-e87b42a3827b', passwordVariable: 'GITLABTOKEN', usernameVariable: 'GITLABTOKEN_USER')]) {
                        pVars['registry_token'] = "${GITLABTOKEN}"
                        pVars['registry_token_user'] = "${GITLABTOKEN_USER}"
                    }

                    dir("Ansible") {
                        configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: 'ansible.cfg',)]) {
                            parallel failFast: true,
                                    environment: {
                                        sh """
                                            git submodule init && git submodule update --remote --merge
                                            cd ../Environments && git checkout ${pVars['client']}
                                            pwd
                                            curl -O https://nexus.geocom.com.uy/repository/Ghiggia/tools/rabbitmq/rabbitmq_delayed_message_exchange-3.12.0.ez
                                        """                                    
                                    }

                            // Run playbook
                            writeYaml data: pVars,
                                    file: 'ansible.vars.yml'

                            
                            ansiblePlaybook(
                                    credentialsId: 'cy5ymCych3DLs29tpAnCC2qF0ze5RdPT',
                                    colorized: true,
                                    disableHostKeyChecking: true,
                                    installation: 'ansible-in-agent',
                                    inventory: "${pVars['env_directory']}/Inventory.ini",
                                    playbook: 'playbook.yml',
                                    tags: params.setup ? 'setup, lazydocker' : '',
                                    extras: "-vv -e @ansible.vars.yml"
                            )
                        }
                    }
                }
            }
        }
    }
}
