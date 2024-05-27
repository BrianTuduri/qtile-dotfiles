import groovy.json.JsonOutput

Map.metaClass.toJson = {
    JsonOutput.toJson(delegate)
}

def file
def targetArtifact = "targetArtifact"

pipeline {
    agent { label 'openfortivpn_nexus_git' }
    options {
        ansiColor('xterm')
        quietPeriod(5)
    }
    environment {
        packagePath = "/tmp/package.zip"
        GIT_SSH_COMMAND="ssh -o StrictHostKeyChecking=no"
    }
    parameters {
        // local parameters
        string(name: 'originBaseNexusURL', defaultValue: 'https://nexus.geocom.com.uy', description: 'URL del repositorio local en HTTPS o SSH de GITLAB (LOCAL)') // local & requerido
        string(name: 'origin_nexus_credentials', defaultValue: 'nexus-geocom-deployment', description: 'Credenciales de NEXUS (REMOTO) en Jenkins.') // LOCAL
        string(name: 'originPath', defaultValue: '', description: 'Path local de el archivo o repositorio (LOCAL)') // local & requerido
        // remote parameters
        string(name: 'remoteBaseNexusURL', defaultValue: 'https://nexus-farmacenter.farmacenter.com.py:4443', description: 'URL del repositorio remoto en HTTPS o SSH de GITLAB (REMOTO)') // local & requerido
        string(name: 'target_nexus_credentials', defaultValue: 'nexus_user_farmacenter', description: 'Credenciales de NEXUS (REMOTO) en Jenkins.') // LOCAL
        string(name: 'targetPath', defaultValue: '', description: 'Path remoto de el archivo o repositorio, por defecto es el mismo (REMOTO)') // local & requerido
        // vpn parameters
        booleanParam(name: 'use_vpn', defaultValue: true, description: 'Indica si necesita una conexion de VPN para llegar a su repositorio GITLAB. En caso de que este sea marcado, debe completar los campos correspondientes. (vpn_host, vpn_port, vpn_credentials)')
        booleanParam(name: 'use_otp', defaultValue: true, description: 'Indica si necesita introducir un codigo OTP de VPN para realizar la conexion VPN).')
        string(name: 'vpn_host', defaultValue: 'ssl.farmacenter.com.py', description: 'Host de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_port', defaultValue: '11443', description: 'Puerto de la VPN a la cual se va a conectar. (En caso de que se necesite).') // VPN
        string(name: 'vpn_credentials', defaultValue: 'farmacenter_vpn', description: 'ID de credenciales en JENKINS de la VPN. (En caso de que se necesite).') // VPN
    }
    stages {
        stage('Validate parameters . . .') {
            steps {
                script {
                    if (!params.originBaseNexusURL.trim() || !params.remoteBaseNexusURL.trim()) {
                        error("El repositorio origen y repositorio destino no pueden estar vacíos.")
                    }
                    if (!params.originPath.trim()) {
                        error("El path de origen y destino no pueden estar vacíos.")
                    }
                    if (!params.target_nexus_credentials.trim() || !params.origin_nexus_credentials.trim()) {
                        error("Las credenciales de Nexus tanto locales como remotas no pueden estar vacías.")
                    }
                }
            }
        }
        stage('Configurando Nexus local . . . ') {
            steps {
                script {
                    nexusConnection(params.originBaseNexusURL, params.origin_nexus_credentials)
                }
            }
        }
        stage('Descargando artefactos de Nexus local . . .') {
            steps {
                script {
                    def artefactos = sh(script: "nexus3 list ${params.originPath}", returnStdout: true).trim().split("\n") 
                    println("Artefactos: ${artefactos}")    
                    // listaArtefactos.each { artefacto ->
                    // }
                    dir (targetArtifact) { 
                        sh "nexus3 download ${params.originPath} ."
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
                    if (!params.vpn_host.trim() || !params.vpn_port.trim() || !params.vpn_credentials.trim()) {
                        error("Cuando se utiliza VPN, los campos [vpn_host, vpn_port, vpn_credentials, nexus_credentials] deben estar completos.")
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
        stage('Configurando Nexus remoto . . . ') {
            steps {
                script {
                    nexusConnection(params.remoteBaseNexusURL, params.target_nexus_credentials)
                }
            }
        }       
        stage('Subiendo artefactos . . .') {
            steps {
                script {
                    dir (targetArtifact) { 
                        srcArtifact = "${params.originPath}".split('/')[1]
                        dstArtifact = params.targetPath ? params.targetPath : "${params.originPath}".split('/')[0]
                        sh "nexus3 upload --recurse ${srcArtifact} ${dstArtifact}/${srcArtifact}"
                    }
                }
            }
        }
    }
}

def nexusConnection(String originBaseNexusURL, String origin_nexus_credentials) {
    withCredentials([usernamePassword(credentialsId: origin_nexus_credentials, usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
        def configFile = "/root/.nexus-cli"
        
        def nexusConfigMap = [
            api_version: "v1",
            groovy_enabled: true,
            password: "${env.NEXUS_PASSWORD}",
            url: originBaseNexusURL,
            username: "${env.NEXUS_USERNAME}",
            x509_verify: true
        ]
        def nexusLocalConnection = JsonOutput.toJson(nexusConfigMap)

        try {
            writeFile(file: configFile, text: nexusLocalConnection)
        } catch (Exception e) {
            error("Failed to write Nexus configuration: ${e.message}")
        }
    }
}