import jenkins.model.Jenkins
import hudson.model.*
import org.biouno.unochoice.AbstractScriptableParameter
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript

// Función para generar la sintaxis del Jenkinsfile para cada tipo de parámetro
def generateJenkinsfileSyntax(ParameterDefinition param) {
    String jenkinsfileEntry = ""
    switch (param) {
        case StringParameterDefinition:
        case TextParameterDefinition:
        case PasswordParameterDefinition:
            jenkinsfileEntry = "string(name: '${param.name}', defaultValue: '${param.defaultValue}', description: '${param.description}')\n"
            break
        case BooleanParameterDefinition:
            jenkinsfileEntry = "booleanParam(name: '${param.name}', defaultValue: ${param.defaultValue}, description: '${param.description}')\n"
            break
        case ChoiceParameterDefinition:
            String choices = param.choices.collect { "'${it}'" }.join(", ")
            jenkinsfileEntry = "choice(name: '${param.name}', choices: [${choices}], description: '${param.description}')\n"
            break
        case AbstractScriptableParameter:
            SecureGroovyScript script = param.getScript().getScript()
            String mainScript = script.getScript()
            boolean sandbox = script.isSandbox()
            jenkinsfileEntry = """activeChoice(name: '${param.name}', script: '''${mainScript}''', sandbox: ${sandbox}, description: '${param.description}')\n"""
            break
    }
    return jenkinsfileEntry
}

// Solicitar el nombre del trabajo desde el usuario o asumir un valor por defecto
def jobName = "Projects/uy-com-geocom-alkosto/geopos2-alkosto"

def jenkinsInstance = Jenkins.instance
def job = jenkinsInstance.getItemByFullName(jobName)

if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
    def parameters = job.getProperty(ParametersDefinitionProperty.class)
    if (parameters) {
        println("parameters {")
        parameters.parameterDefinitions.each { param ->
            String jenkinsfileSyntax = generateJenkinsfileSyntax(param)
            println(jenkinsfileSyntax)
        }
        println("}")
    } else {
        println("No parameters found for job: $jobName")
    }
} else {
    println("$jobName is not a parameterized job.")
}
