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

                    dir ('repository/pipelines_in_testing') { 
                        def params = load 'defineParameters.groovy' // lista
                        properties([parameters(params)])
                    }
                }
            }
        }
        stage('Parameters . . .') {
            steps {
                script {
                    def repo_url = StringEscapeUtils.unescapeHtml(params.repo_url)

                    echo "Versión Maven: ${params.mvn_version}"
                    echo "Versión Java: ${params.java_version}"
                    echo "Ambiente: ${params.ambiente}"
                    echo "Tienda: ${params.store}"
                    echo "Instalación: ${params.INSTALL}"
                    echo "Host particular: ${params.particular_host}"
                    echo "Path de el repositorio: ${params.projectPathWithNamespace}"
                    echo "ID de repositorio: ${params.repo_id}"
                    echo "URL del repositorio: ${repo_url}"
                    echo "Rama Git seleccionada: ${params.branch}"
                }
            }
        }
    }
}
