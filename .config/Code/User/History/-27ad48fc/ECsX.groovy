import jenkins.model.Jenkins
import com.cloudbees.hudson.plugins.folder.Folder

pipeline {
    agent { label "ansible-ssh" }
    options {
        ansiColor('xterm') // Interpretación de colores ANSI con esquema de colores 'xterm'.
        
        // Rotación de registros y artefactos. Se mantendrán los últimos 5 registros de compilación.
        buildDiscarder logRotator(
            artifactDaysToKeepStr: '',  // Días para mantener los artefactos (vacío significa sin límite)
            artifactNumToKeepStr: '',   // Número de artefactos para mantener (vacío significa sin límite)
            daysToKeepStr: '',          // Días para mantener los registros de compilación (vacío significa sin límite)
            numToKeepStr: '5'           // Número de registros de compilación para mantener
        )
        disableConcurrentBuilds() // Deshabilita la ejecución concurrente de más de una compilación del mismo trabajo.
        quietPeriod(5) // Establece un período de silencio de 5 segundos antes de que comience la compilación.
    }

    stages {
        stage('Create folder . . .') {
            steps {
                script {     
                    createFoldersAndJob("/Projects/a/b/c") 
                }
            }
        }
    }

}


def createFoldersAndJob(String fullPath) {
    // Extraer las partes del path y el nombre del job
    def parts = fullPath.tokenize('/')
    parts.pop() // Eliminar el nombre del job del final del path

    // Instancia de Jenkins
    Jenkins jenkins = Jenkins.getInstance()

    // Iniciar desde la raíz del Jenkins, que es de tipo ItemGroup pero no Folder
    def parent = jenkins.getItemByFullName("") // Esto debe devolver la raíz de Jenkins

    parts.each { part ->
        // Intentar obtener el Folder existente
        Folder folder = parent.getItem(part) ?: null
        if (folder == null) {
            // Crear y configurar un nuevo Folder si no existe
            folder = new Folder(parent, part)
            folder.save() // Guardar el Folder
            jenkins.reload() // Recargar configuración de Jenkins para asegurar la visibilidad del nuevo Folder
        }
        parent = folder // Establecer el Folder actual como el último, para la siguiente iteración
    }
}