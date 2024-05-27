return [
    choice(name: 'mvn_version', choices: ['Maven_2', 'Maven_3'], description: 'Selecciona la versión de Maven'),
    choice(name: 'java_version', choices: ['JDK_1.6', 'JDK_1.8'], description: 'Selecciona la versión de Java'),
    booleanParam(name: 'INSTALL', defaultValue: false, description: 'Lanzar Instalación automática'),
    choice(name: 'ambiente', choices: ['', 'qa_geocom', 'release'], description: 'Selecciona el ambiente'),
    choice(name: 'store', choices: ['qa_geocom', 'qa_geocom_internet', 'release', 'massive'], description: 'Selecciona la tienda'),
    string(name: 'particular_host', defaultValue: 'null', description: 'Host específico si es necesario'),
    choice(name: 'Environment', choices: ["Select:selected", "DEV", "TEST", "STAGE", "PROD"], description: ''),
    [
        $class: 'ChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'Environment',
        randomName: 'choice-parameter-5633384460832175', 
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId:'Environments.groovy'
        ]
    ],
    [
        $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        name: 'Host',
        referencedParameters: 'Environment',
        randomName: 'choice-parameter-5633asdad832175', 
        script: [
            $class: 'ScriptlerScript',
            scriptlerScriptId:'HostsInEnv.groovy',
            parameters: [
                [name:'Environment', value: '$Environment']
            ]
        ]
    ]
]
