// set project name
def projectPathWithNamespace = "uy-com-geocom-scm/devops/release-automation-pipelines"

// select maven or gradle
def mavenProject = false
def gradleProject = false

def commonsParameters = [
    choice(name: 'projectPathWithNamespace', choices: [projectPathWithNamespace], description: 'Path completo al proyecto (incluyendo namespace) Ej: uy-com-geocom-scm/devops/release-automation-pipelines'),
    [  // parameter: repo_id
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'repo_id', 
        randomName: UUID.randomUUID().toString(), 
        referencedParameters: 'projectPathWithNamespace',
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId:'getRepoId.groovy',
            fallbackScript: [ classpath: [], script: 'return ["N/A"]'],
        ]
    ], // parameter: repo_id
    [  // parameter: repo_url
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'repo_url',
        randomName: UUID.randomUUID().toString(), 
        referencedParameters: 'repo_id',
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId:'getRepoUrl.groovy',
            fallbackScript: [ classpath: [], script: 'return ["N/A"]'],
        ]
    ], // parameter: repo_url
    [  // paramter: branch
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'branch',
        filterLength: 1, 
        filterable: true, 
        randomName: UUID.randomUUID().toString(), 
        referencedParameters: 'repo_id',
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId:'getBranches.groovy',
            fallbackScript: [ classpath: [], script: 'return ["N/A"]'],
        ]
    ]  // paramter: branch
]

def buildParameters = [
    choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8'], description: 'Selecciona la versión de Java'),
    booleanParam(name: 'wrapper', defaultValue: true, description: 'Seleccione si el proyecto utiliza wrapper')
]

if (mavenProject) {
    buildParameters = buildParameters.addAll([
        choice(name: 'buildToolVersion', choices: ['Maven_2', 'Maven_3'], description: 'Selecciona la versión de Maven'),
        hidden(name: 'release_param', defaultValue: '-Dresume=false release:prepare release:perform', description: 'Hidden parameter'),
        hidden(name: 'build_param', defaultValue: 'clean deploy', description: 'Hidden parameter')
    ])
}

if (gradleProject) {
    buildParameters = buildParameters.addAll([
        hidden(name: 'release_param', defaultValue: 'release -Prelease.useAutomaticVersion=true', description: 'Hidden parameter'),
        hidden(name: 'build_param', defaultValue: 'build', description: 'Hidden parameter')
    ])
}

def Parameters = commonsParameters.addAll(buildParameters)

return Parameters // lista

//
// Para alkosto se puede agregar instalar directamente
//
    // booleanParam(name: 'INSTALL', defaultValue: false, description: 'Lanzar Instalación automática'),
    // choice(name: 'ambiente', choices: ['', 'qa_geocom', 'release'], description: 'Selecciona el ambiente'),
    // choice(name: 'store', choices: ['qa_geocom', 'qa_geocom_internet', 'release', 'massive'], description: 'Selecciona la tienda'),
    // string(name: 'particular_host', defaultValue: 'null', description: 'Host específico si es necesario'),
