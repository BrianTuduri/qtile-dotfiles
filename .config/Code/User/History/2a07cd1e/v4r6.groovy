// Función para combinar configuraciones
def mergeConfigs(baseConfig, overrideConfig) {
    overrideConfig.each { key, value ->
        def path = key.split("\\.") as List
        def lastKey = path.pop()

        def current = baseConfig
        path.each { part ->
            if (!current.containsKey(part)) {
                current[part] = [:]  // Crear un nuevo mapa si no existe la parte
            } else if (!(current[part] instanceof Map)) {
                current[part] = [:]  // Sobrescribir con un mapa si el tipo existente no es un mapa
            }
            current = current[part]
        }
        
        if (current[lastKey] instanceof Map && value instanceof Map) {
            current[lastKey] = mergeConfigs(current[lastKey], value)  // Llamada recursiva para fusionar mapas
        } else {
            current[lastKey] = value  // Sobrescribir el valor
        }
    }
    return baseConfig
}

// Función para validar si un mapa tiene valores vacíos y aceptar excepciones
def validateConfig(Map config, List ignoreList = []) {
    // Método recursivo para explorar y validar el mapa
    def checkMap(Map item, List path = []) {
        item.each { key, value ->
            // Construir el path actual de la clave
            def currentPath = path + [key]
            if (value instanceof Map) {
                // Si el valor es otro mapa, llamada recursiva
                checkMap(value, currentPath)
            } else {
                // Si el valor está vacío y no está en la lista de ignorados
                if ((value == null || value.trim() == '') && !ignoreList.contains(currentPath.join('.'))) {
                    throw new RuntimeException("Config value missing at ${currentPath.join('.')} which is not allowed to be empty")
                }
            }
        }
    }
    // Iniciar la validación del mapa
    checkMap(config)
}

// Mapa de configuración base de prueba
def baseConfig = [
    docker: [
        image: "nexus.geocom.com.uy/scm-docker-images/openjdk:mvn2.2.1-jdk8",
        workdir: "/project",
        volumes: ["/slave/m2/:/root/.m2/"],
        args: ["--add-host 'gitlab.geocom.com.uy:172.24.18.194'"]
    ],
    jenkins: [
        credentials: [
            DOCKER_IP_CREDENTIAL: 'docker_slave_ip_port',
            DOCKER_SERVER_CREDENTIAL: 'Docker_Certs',
            NEXUS_IP_CREDENTIAL: 'nexus_slave_ip_port',
            GIT_CREDENTIAL_ID: "15940393-32ad-416e-ad80-b8ea71536641"
        ]
    ],
    nexus: [
        credentials: [
            NEXUS_REPOSITORY: "Ghiggia-60-dias",
            NEXUS_GROUP_ID: "uy.com.geocom.alkosto",
            NEXUS_CREDENTIAL_ID: "nexus-deploy"
        ]
    ],
    git: [
        LABEL: "jdk21"
    ]
]

// Mapa de anulación de prueba
def overrideConfig = [
    "docker.image": "fdsfgsdfsfsf",
    "docker.workdir": "/test",
    //"docker.volumes": ["asdasd", "adsada"]
]

// Lista de claves a ignorar
def ignoreList = ["jenkins.credentials.DOCKER_IP_CREDENTIAL", "jenkins.credentials.GIT_CREDENTIAL_ID", "git.LABEL"]

try {
    validateConfig(baseConfig, ignoreList)
} catch (RuntimeException e) {
    println e.getMessage()
}

// Llamar a la función para combinar los mapas
//def mergedConfig = mergeConfigs(baseConfig, overrideConfig)

//def volumesJoin = "-v ${mergedConfig.docker.volumes.join(' -v ')}"

// Imprimir el resultado para verificar que la anulación funciona correctamente
println "Configuración final: ${mergedConfig}"
