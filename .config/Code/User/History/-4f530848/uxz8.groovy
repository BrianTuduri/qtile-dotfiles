pipeline{
    agent { label 'openfortivpn' }
    options {
        ansiColor('xterm')
        quietPeriod(5)
    }
    environment {
        GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no"
    }
    parameters {
        // local
        string(name: 'repo_origin', defaultValue: '', description: 'URL del repositorio local en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        string(name: 'target_repository', defaultValue: '', description: 'URL del repositorio remoto en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        booleanParam(name: 'specify_branches', defaultValue: false, description: 'Booleano que permite especificar ramas a migrar. En caso de que este sea marcado, debe completar el campo [branches].')
        string(name: 'branches', defaultValue: '', description: 'Lista separada por comas de las ramas que desea migrar. (En caso de que seleccione specify_branches).') 
        booleanParam(name: 'use_vpn', defaultValue: true, description: 'Indica si necesita una conexion de VPN para llegar a su repositorio GITLAB. En caso de que este sea marcado, debe completar los campos correspondientes. (vpn_host, vpn_port, vpn_credentials)')
        booleanParam(name: 'use_otp', defaultValue: false, description: 'Indica si necesita introducir un codigo OTP de VPN para realizar la conexion VPN).')
        // vpn
        string(name: 'vpn_host', defaultValue: 'vpn.gamaitaly.cl', description: 'Host de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_port', defaultValue: '443', description: 'Puerto de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_credentials', defaultValue: 'gama_vpn', description: 'ID de credenciales en JENKINS de la VPN. (En caso de que se necesite).') // VPN
        // remote
        string(name: 'gitlab_remote_credentials', defaultValue: 'gitlab-gama-root', description: 'Credenciales de GITLAB (REMOTO) en Jenkins.') // REMOTO

    }
    stages {
        stage('Validate parameters . . .') {
            steps {
                script {
                    if (!params.repo_origin.trim() || !params.target_repository.trim()) {
                        error("El repositorio origen y repositorio destino no pueden estar vacíos.")
                    }
                    if (params.specify_branches && params.branches.trim().isEmpty()) {
                        error("Ha seleccionado especificar ramas, pero no ha indicado ninguna en el campo branches.")
                    }
                }
            }
        }

        stage('Conectarme a la VPN . . .') {
            when {
                expression {
                    return params.use_vpn == true
                }
            }
            steps {
                script {
                    if (!params.vpn_host.trim() || !params.vpn_port.trim() || !params.vpn_credentials.trim() || !params.gitlab_remote_credentials.trim()) {
                        error("Cuando se utiliza VPN, los campos [vpn_host, vpn_port, vpn_credentials, gitlab_remote_credentials] deben estar completos.")
                    }

                    withCredentials([usernamePassword(credentialsId: params.vpn_credentials, usernameVariable: 'VPN_USERNAME', passwordVariable: 'VPN_PASSWORD')]) {
                        println("Execute openfortivpn . . .")
                        try {
                            def dataExport = "export VPN_HOST=${params.vpn_host} && export VPN_PORT=${params.vpn_port}" 
                            if (params.use_otp) {
                                def otpInput = input(id: 'userInput', message: 'Ingrese el código OTP de su aplicación', parameters: [string(name: 'OTP', defaultValue: '', description: 'Código OTP')])
                                otpInput ?: error("El código OTP no puede estar vacio")
                                dataExport += " export VPN_OTP=${otpInput}"
                            }
                            sh "${dataExport} && trusted_openfortivpn.sh"
                        } catch(Exception e) {
                            def log = sh(script: "cat /tmp/openfortivpn.log", returnStdout: true)
                            error("LOG: ${log} y el error ${e}")
                        }
                    }
                }
            }
        }
        stage('Upload') {
            steps {
                script {
                        def remoteName = "origin"
                        sh '''
                            git config --global user.email scm2@geocom.com.uy
                            git config --global user.name 'jenkins'
                            git config pull.rebase true
                        '''
                        sh "mkdir tmp"
                        sshagent(['15940393-32ad-416e-ad80-b8ea71536641']) {
                            sh "git clone ${params.repo_origin} --bare tmp"
                        }
                        
                    sshagent([params.gitlab_remote_credentials]) { // ambiente_gama_private_key
                        dir ("tmp") {
                            sh """
                                git remote remove ${remoteName}
                                git remote add ${remoteName} ${params.target_repository}
                            """

                            if (params.specify_branches) {
                                def lista = "${params.branches}".split(',')
                                // Push specific branches
                                for (branch in lista) {
                                    sh "git push --force -u ${remoteName} ${branch}:${branch}"
                                }
                            } else {
                                // Push all branches
                                sh """
                                    git  push ${remoteName} --all --force
                                    git  push ${remoteName} --tags --force
                                """
                            }
                        }
                    }
                }
            }
        }
    }
}