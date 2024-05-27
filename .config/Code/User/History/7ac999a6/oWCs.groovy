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
        description: 'Select the Server from the Dropdown List', 
        filterable: true, 
        name: 'Server', 
        randomName: 'choice-parameter-5631314456178619', 
        referencedParameters: 'Env', 
        script: [
            $class: 'org.biouno.unochoice.GroovyScript', 
            fallbackScript: [
                script: 'return ["Could not get Environment from Env Param"]',
                sandbox: true
            ], 
            script: [
                script: '''
                    if (Env.equals("Dev")){
                        return ["devaaa001", "devaaa002", "devbbb001", "devbbb002", "devccc001", "devccc002"]
                    } else if (Env.equals("QA")){
                        return ["qaaaa001", "qabbb002", "qaccc003"]
                    } else if (Env.equals("Stage")){
                        return ["staaa001", "stbbb002", "stccc003"]
                    } else if (Env.equals("Prod")){
                        return ["praaa001", "prbbb002", "prccc003"]
                    } else {
                        return ["Select valid Env"]
                    }
                ''',
                sandbox: true
            ]
        ]
    ]
]
