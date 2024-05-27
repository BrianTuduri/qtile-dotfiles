pipeline {
    agent any
    stages {
        stage('Setup') {
            steps {
                script {
                    checkout scm
                    def params = load 'defineParameters.groovy'
                    properties([parameters(params)])
                }
            }
        }
        stage('Build') {
            steps {
                echo "Versión Maven: ${params.mvn_version}"
                echo "Versión Java: ${params.java_version}"
                echo "Ambiente: ${params.ambiente}"
                echo "Tienda: ${params.store}"
                echo "Instalación: ${params.INSTALL}"
                echo "Host particular: ${params.particular_host}"
                echo "Rama Git seleccionada: ${params.branch}"
            }
        }
    }
}
