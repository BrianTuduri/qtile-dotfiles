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
        NEXUS_REPOSITORY: "Ghiggia-60-dias",
        NEXUS_GROUP_ID: "uy.com.geocom.alkosto",
        NEXUS_CREDENTIAL_ID: "nexus-deploy"
    ],
    git: [
        LABEL: "jdk21"
    ]
]

// Función para combinar configuraciones
def mergeConfigs(baseConfig, overrideConfig) {
    overrideConfig.each { key, value ->
        def path = key.split("\\.")
        def lastKey = path.pop()

        def current = baseConfig
        path.each { part ->
            if (!current.containsKey(part)) {
                current[part] = [:]
            }
            current = current[part]
        }
        current[lastKey] = value
    }
    return baseConfig
}


// Mapa de anulación de prueba
def overrideConfig = [
    "docker.image": "fdsfgsdfsfsf"
]

// Llamar a la función para combinar los mapas
def mergedConfig = mergeConfigs(baseConfig, overrideConfig)

// Imprimir el resultado para verificar que la anulación funciona correctamente
println "Configuración final: ${mergedConfig}"
