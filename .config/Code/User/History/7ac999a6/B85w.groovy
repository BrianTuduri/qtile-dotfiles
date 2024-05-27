
// PARAMETROS GEOPOS2-ALKOSTO

// Definir el camino del proyecto con el espacio de nombres
def projectPathWithNamespace = "uy-com-geocom-scm/devops/release-automation-pipelines"

// Definición inicial de parámetros comunes
def commonsParameters = [
    choice(name: 'projectPathWithNamespace', choices: [projectPathWithNamespace], description: 'Path completo al proyecto (incluyendo namespace)'),
 // cascadeChoiceParameter( name,      scriptId,          referencedParam)
    cascadeChoiceParameter('repo_id', 'getRepoId.groovy', 'projectPathWithNamespace'),
    cascadeChoiceParameter('repo_url', 'getRepoUrl.groovy', 'repo_id'),
    cascadeChoiceParameter('branch', 'getBranches.groovy', 'repo_id')
]

// Parámetros para la construcción, seleccionables según el tipo de proyecto
def buildParameters = [
    choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8', 'JDK_21'], description: 'Selecciona la versión de Java'),
    booleanParam(name: 'wrapper', defaultValue: true, description: 'Seleccione si el proyecto utiliza wrapper'),
    choice(name: 'buildEnvironment', choices: ['None', 'Maven', 'Gradle'], description: 'Seleccione el entorno de compilación')
]

// Agregar parámetros específicos de Maven o Gradle según la selección
buildParameters = buildParameters + getBuildEnvironmentParams()

// Combinar parámetros comunes y de construcción
def allParameters = commonsParameters + buildParameters

return allParameters // Retornar la lista de parámetros

// Funciones auxiliares para simplificar la creación de parámetros
def cascadeChoiceParameter(name, scriptId, referencedParam) {
    [
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: name,
        randomName: UUID.randomUUID().toString(),
        referencedParameters: referencedParam,
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId: scriptId,
            fallbackScript: [classpath: [], script: 'return ["N/A"]']
        ]
    ]
}

def getBuildEnvironmentParams() {
    [
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'buildToolVersion',
            randomName: UUID.randomUUID().toString(),
            visible: { params -> params.buildEnvironment == 'Maven' },
            choices: ['Maven_2', 'Maven_3'],
            description: 'Selecciona la versión de Maven'
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'buildToolVersion',
            randomName: UUID.randomUUID().toString(),
            visible: { params -> params.buildEnvironment == 'Gradle' },
            choices: ['Gradle_5', 'Gradle_6'],
            description: 'Selecciona la versión de Gradle'
        ]
    ]
}
