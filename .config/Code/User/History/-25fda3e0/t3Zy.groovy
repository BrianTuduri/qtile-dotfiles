pipeline {
    agent { label 'openfortivpn' }
    options {
        ansiColor('xterm')
        quietPeriod(5)
    }
    environment {
        GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no"
    }
    parameters {
        string(name: 'ORIGIN_SERVER', defaultValue: 'https://jenkins.geocom.com.uy', description: 'URL del repositorio local en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        string(name: 'ORIGIN_CREDENTIALS', defaultValue: 'api_token_jenkins_bt', description: 'Credenciales de JENKINS (LOCAL) en Jenkins.') // REMOTO
        string(name: 'ORIGIN_JOB_PATH', defaultValue: 'devops/scm-infra/base-server-install', description: 'URL del repositorio local en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        string(name: 'DEST_SERVER', defaultValue: 'https://jenkins-gama.geocom.com.uy:9443', description: 'URL del repositorio remoto en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        string(name: 'DEST_CREDENTIALS', defaultValue: 'jenkins_gama_token', description: 'Credenciales de JENKINS (REMOTO) en Jenkins.') // REMOTO
        string(name: 'DEST_JOB_PATH', defaultValue: 'devops/scm-infra/base-server-install', description: 'URL del repositorio remoto en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        booleanParam(name: 'use_vpn', defaultValue: true, description: 'Indica si necesita una conexion de VPN para llegar a su repositorio GITLAB. En caso de que este sea marcado, debe completar los campos correspondientes. (vpn_host, vpn_port, vpn_credentials)')
        booleanParam(name: 'use_otp', defaultValue: false, description: 'Indica si necesita introducir un codigo OTP de VPN para realizar la conexion VPN).')
        string(name: 'vpn_host', defaultValue: 'vpn.gamaitaly.cl', description: 'Host de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_port', defaultValue: '443', description: 'Puerto de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_credentials', defaultValue: 'gama_vpn', description: 'ID de credenciales en JENKINS de la VPN. (En caso de que se necesite).') // VPN
    }
    stages {
        stage('Validate parameters . . .') {
            steps {
                script {
                    if (!params.ORIGIN_SERVER.trim() || !params.DEST_SERVER.trim()) {
                        error("El servidor de Jenkins origen y el servidor destino no pueden estar vacíos.")
                    }
                    if (params.ORIGIN_JOB_PATH.trim() && params.DEST_JOB_PATH.trim().isEmpty()) {
                        error("El path inicial y destino de los job no pueden estar vacíos.")
                    }
                }
            }
        }
        stage('Descargar Jenkins CLI') {
            steps {
                script {
                    echo "Descargando jenkins-cli.jar desde nexus"
                    sh """
                        wget https://nexus.geocom.com.uy/repository/Ghiggia/tools/jenkins-cli/jdk-8/jenkins-cli.jar
                    """
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
                    if (!params.vpn_host.trim() || !params.vpn_port.trim() || !params.vpn_credentials.trim() || !params.DEST_CREDENTIALS.trim()) {
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

        stage('Exportar job desde servidor origen') {
            steps {
                script {
                    def tempFile = "${params.ORIGIN_JOB_PATH}-${currentBuild.number}.xml".replaceAll( "/", "-")
                    try {
                        println("Exportando el job ${params.ORIGIN_JOB_PATH} desde ${params.ORIGIN_SERVER} . . .")
                        withCredentials([usernamePassword(credentialsId: "${params.ORIGIN_CREDENTIALS}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                        sh """
                            java -jar jenkins-cli.jar -s ${params.ORIGIN_SERVER} -auth ${USERNAME}:${PASSWORD} get-job ${params.ORIGIN_JOB_PATH} > ${tempFile}
                        """
                        }
                        println("Job exportado exitosamente.")
                    } catch (Exception e) {
                        println("Error al exportar el job desde el servidor origen.")
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }

        stage('Importar job al servidor destino') {
            steps {
                script {
                    def tempFile = "${params.ORIGIN_JOB_PATH}-${currentBuild.number}.xml".replaceAll("/", "-")
                    try {
                        println("Importando job a ${params.DEST_SERVER}")
                        withCredentials([usernamePassword(credentialsId: "${params.DEST_CREDENTIALS}", passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                        sh """
                            java -jar jenkins-cli.jar -s ${params.DEST_SERVER} -auth ${USERNAME}:${PASSWORD} create-job ${params.DEST_JOB_PATH} < ${tempFile}
                        """
                        }
                        println("Job ${params.ORIGIN_JOB_PATH} importado exitosamente en ${params.DEST_JOB_PATH}.")
                    } catch (Exception e) {
                        println("Error al importar el job ${params.ORIGIN_JOB_PATH} al servidor destino.")
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }
    }
}
