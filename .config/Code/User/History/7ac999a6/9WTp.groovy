return [
    choice(name: 'mvn_version', choices: ['Maven_2', 'Maven_3'], description: 'Selecciona la versión de Maven'),
    choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8'], description: 'Selecciona la versión de Java'),
    booleanParam(name: 'INSTALL', defaultValue: false, description: 'Lanzar Instalación automática'),
    choice(name: 'ambiente', choices: ['', 'qa_geocom', 'release'], description: 'Selecciona el ambiente'),
    choice(name: 'store', choices: ['qa_geocom', 'qa_geocom_internet', 'release', 'massive'], description: 'Selecciona la tienda'),
    string(name: 'particular_host', defaultValue: 'null', description: 'Host específico si es necesario'),
    [
        $class: 'org.biouno.unochoice.CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'Branch',
        description: 'Select a Git branch',
        script: [
            $class: 'org.biouno.unochoice.ScriptlerScript',
            scriptlerScriptId: 'getBranchesTest.groovy',
            // parameters: [
            //
            // ]
        ]
    ]
]
