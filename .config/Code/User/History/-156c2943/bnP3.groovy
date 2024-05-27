pipeline {
    agent any
    parameters {
        choice(name: 'TAREA', choices: ['release', 'regenerate_version', 'deploy'], description: 'Seleccione la tarea a realizar')
        string(name: 'DOCKER_IMAGE', defaultValue: 'maven:3.6.3-jdk-11', description: 'Imagen de Docker a usar')
        string(name: 'DOCKER_SERVER_URL', defaultValue: 'tcp://docker-server:2376', description: 'URL del servidor Docker')
    }
    environment {
        MVN_VOLUME = "/home/jenkins/.m2:/root/.m2"
        PROJECT_VOLUME = "${WORKSPACE}:/proyecto"
    }
    stages {
        stage('Preparar y Ejecutar en Docker') {
            steps {
                script {
                    def mavenCommands = getMavenCommands(params.TAREA)
                    docker.withServer(params.DOCKER_SERVER_URL) {
                        docker.withTool('my-docker') {
                            docker.image(params.DOCKER_IMAGE).inside("-v ${MVN_VOLUME} -v ${PROJECT_VOLUME}") {
                                sh "mvn ${mavenCommands}"
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Finalizando ejecución, limpieza o pasos adicionales si son necesarios."
        }
    }
}

def getMavenCommands(String tarea) {
    switch(tarea) {
        case 'release':
            return "-Dresume=false release:prepare release:perform"
        case 'regenerate_version':
            return "clean package"
        default:
            return "clean deploy"
    }
}