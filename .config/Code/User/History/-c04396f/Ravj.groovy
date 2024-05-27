////////////////////        CONFIGURAR    ////////////////////////////////////
def projectPathWithNamespace = "uy-com-geocom-alkosto/geopos2-refactor-alkosto"
def mavenProject = true
def gradleProject = false
////////////////////////////////////////////////////////////////////////////////////////


if (mavenProject && gradleProject) {
    throw new IllegalArgumentException("Solo uno de los proyectos (Maven o Gradle) puede estar activo a la vez.")
}

def parameters = defineCommonParameters(projectPathWithNamespace) + defineBuildParameters(mavenProject, gradleProject)

return parameters

def defineCommonParameters(String pathWithNamespace) {
    [
        choice(name: 'projectPathWithNamespace', choices: [pathWithNamespace], description: 'Path completo al proyecto (incluyendo namespace)'),
    //                          name        scriptId           referencedParam
        cascadeChoiceParameter('repo_id', 'getRepoId.groovy', 'projectPathWithNamespace'),
        cascadeChoiceParameter('repo_url', 'getRepoUrl.groovy', 'repo_id'),
        cascadeChoiceParameter('branch', 'getBranches.groovy', 'repo_id')
    ]
}

def defineBuildParameters(boolean maven, boolean gradle) {
    def buildParameters = [
        choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8'], description: 'Selecciona la versión de Java'),
        booleanParam(name: 'wrapper', defaultValue: false, description: 'Seleccione si el proyecto utiliza wrapper')
    ]
    if (maven) {
        buildParameters += mavenSpecificParameters()
    }
    if (gradle) {
        buildParameters += gradleSpecificParameters()
    }
    return buildParameters
}

def cascadeChoiceParameter(String name, String scriptId, String referencedParam) {
    filter = false
    if (name == 'branch' | name == 'branches') { filter = true }
    [
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: name,
        filterLength: 1, 
        filterable: filter,
        randomName: UUID.randomUUID().toString(),
        referencedParameters: referencedParam,
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId: scriptId,
            fallbackScript: [classpath: [], script: 'return ["N/A"]']
        ]
    ]
}

// toDO: AGREGAR PARAMETROS DE ARGS DE BUILD PARA MAVEN O GRADLE

def mavenSpecificParameters() {
    [
        choice(name: 'buildToolVersion', choices: ['Maven_3.6', 'Maven_3.8'], description: 'Selecciona la versión de Maven'),
        choice(name: 'typeBuild', choices: ['JDK_1.6', '.JDK_18'], description: 'Selecciona la versión de Java'),
        choice(name: 'tool', choices: ['Maven'], description: 'Type tool Hidden parameter (Maven or Gradle)'),
        hidden(name: 'hidden_param', defaultValue: 'hidden_value', description: 'Hidden parameter')
    ]
}

def gradleSpecificParameters() {
    [
        choice(name: 'buildToolVersion', choices: ['Gradle_6', 'Gradle_7', 'Gradle_8'], description: 'Selecciona la versión de Gradle'),
        choice(name: 'tool', choices: ['Gradle'], description: 'Type tool Hidden parameter (Maven or Gradle)'),
        hidden(name: 'hidden_param', defaultValue: 'hidden_value', description: 'Hidden parameter')
    ]
}
