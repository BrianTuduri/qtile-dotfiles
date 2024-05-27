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
        choice(name: 'kafka_version', choices: ['3.6.1'], description: 'Select Kafka version')
        choice(name: 'kafka_scala_version', choices: ['2.13'], description: 'Select Scala version')
        choice(name: 'kafka_openjdk_version', choices: ['21'], description: 'Select openjdk version')
        choice(name: 'ambiente', choices: ['CERT'], description: 'Pick something')
        choice(name: 'instanceEnv', choices: ['GAMA'], description: 'Pick something')

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
        stage("Set settings . . .") {
            steps {
                script {
                switch(params.instanceEnv) {
                    case "GAMA":
                        gitUrl = [
                            "geoscm-ansible-collection" : "git@gitlab-gama.geocom.com.uy:scm/geoscm-ansible-collection.git",
                            "kubernetes-ansible-collection" : "git@gitlab-gama.geocom.com.uy:scm/DevOPS/kubernetes-ansible-collection.git"
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
                }
            }
        }
        stage('Ansible . . .') {
            steps {
                script {
                    println(params.tags)
                    def selectedTags = params.tags.tokenize(',')
                    def ansible_tags = selectedTags.join(',')
                    dir('ansible') {
                        //sshagent(['cy5ymCych3DLs29tpAnCC2qF0ze5RdPT']) { // root (Clave privada para ambientes QA)
                            sshagent([sshCreds]) { // jenkins (Clave pare construcciones GitLab)
                                configFileProvider([configFile(fileId: 'ansible_agent_cfg', targetLocation: "ansible.cfg")]) {
                                    sh "ansible-galaxy install -r requirements.yml"
                                    ansiblePlaybook(
                                        installation: 'Ansible',
                                        playbook: 'playbooks/install_kafka/main.yml',
                                        inventory: "inventory.ini",
                                        credentialsId: '15940393-32ad-416e-ad80-b8ea71536641', // jenkins (Clave pare construcciones GitLab)
                                        extraVars: [
                                            repo_home: "${pwd()}",
                                            ambiente: "${params.ambiente}",
                                            ansible_user: "geocom"
                                        ],
                                        tags: "${ansible_tags}",
                                        extras: "-e '@ansible.vars.yml'",
                                        colorized: true
                                    )
                                }
                            }
                        //}
                    }
                }    
            }
        }
    }
}