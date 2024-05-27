return [
    choice(name: 'projectPathWithNamespace', choices: ['uy-com-geocom-alkosto/geopos2-refactor-alkosto'], description: 'Path completo al proyecto (incluyendo namespace) Ej: uy-com-geocom-scm/devops/release-automation-pipelines'),
    [
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
    ],
    [
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
    ],
    [
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
    ],
    choice(name: 'mvn_version', choices: ['Maven_2', 'Maven_3'], description: 'Selecciona la versión de Maven'),
    choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8'], description: 'Selecciona la versión de Java'),
    booleanParam(name: 'INSTALL', defaultValue: false, description: 'Lanzar Instalación automática'),
    choice(name: 'ambiente', choices: ['', 'qa_geocom', 'release'], description: 'Selecciona el ambiente'),
    choice(name: 'store', choices: ['qa_geocom', 'qa_geocom_internet', 'release', 'massive'], description: 'Selecciona la tienda'),
    string(name: 'particular_host', defaultValue: 'null', description: 'Host específico si es necesario')
]
