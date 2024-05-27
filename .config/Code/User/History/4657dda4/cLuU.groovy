pipeline {
    agent { label 'jdk21' }
    
    stages {
        stage('Setup') {
            steps {
                script {
                    checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                        branches: [[name: "*/master"]], doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true],
                        [$class: 'LocalBranch', localBranch: "master"],
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: 'repository']],
                        gitTool: 'jgit', submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: '15940393-32ad-416e-ad80-b8ea71536641',
                        url: 'git@gitlab.geocom.com.uy:uy-com-geocom-scm/devops/release-automation-pipelines.git']]]

                    dir ('repository/pipelines_in_testing/defineParameters.groovy') { def params = load 'defineParameters.groovy' }
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
