import hudson.model.*
import hudson.maven.MavenModuleSet
import java.time.*
import jenkins.model.Jenkins
import hudson.plugins.git.GitSCM

// Parámetros configurables
String sortField = "time" // "time" o "name"
String sortOrder = "desc" // "asc" o "desc"
int timeValue = 6 // cantidad de tiempo
String timeUnit = "months" // "years", "months" o "days"

boolean isActiveJob(Job job, int value, String unit) {
    if (job.getLastBuild() != null) {
        long lastBuildMillis = job.getLastBuild().getTimeInMillis()
        ZonedDateTime targetDate = ZonedDateTime.now()
        switch (unit) {
            case "years":
                targetDate = targetDate.minusYears(value)
                break;
            case "months":
                targetDate = targetDate.minusMonths(value)
                break;
            case "days":
                targetDate = targetDate.minusDays(value)
                break;
        }
        long targetMillis = targetDate.toInstant().toEpochMilli()
        return lastBuildMillis > targetMillis
    }
    return false
}

def mavenJobs = Jenkins.instance.getAllItems(MavenModuleSet.class).findAll { job -> 
    isActiveJob(job, timeValue, timeUnit)
}

if (sortField == "time") {
    if (sortOrder == "asc") {
        mavenJobs.sort { it.getLastBuild().getTimeInMillis() }
    } else {
        mavenJobs.sort { -it.getLastBuild().getTimeInMillis() }
    }
} else if (sortField == "name") {
    if (sortOrder == "asc") {
        mavenJobs.sort { it.name }
    } else {
        mavenJobs.sort { -it.name.bytes }
    }
}

println "Trabajos Maven activos (últimos ${timeValue} ${timeUnit}): ${mavenJobs.size()}"

mavenJobs.each { job ->
    def rootUrl = Jenkins.instance.getRootUrl()
    def jobUrl = job.getUrl()
    def fullUrl = "${rootUrl}${jobUrl}"
    def mavenVersion = job.getMaven().getName()
    def jdk = job.getJDK() ? job.getJDK().getName() : "Default JDK"
    
    def scmUrl = "No SCM configurado"
    if (job.getScm() instanceof GitSCM) {
        scmUrl = (job.getScm() as GitSCM).getUserRemoteConfigs().first()?.getUrl() ?: "No SCM configurado"
    }
    
    def uniqueUsers = []
    job.getBuilds().each { build ->
        if (uniqueUsers.size() < 5) {
            def user = build.getCauses().find { cause -> cause instanceof Cause.UserIdCause }?.getUserName() ?: "Desconocido"
            if (!uniqueUsers.contains(user)) {
                uniqueUsers.add(user)
            }
        }
    }
    
    def jobParameters = job.getProperty(ParametersDefinitionProperty.class)?.getParameterDefinitions().collect { param ->
        "${param.getName()}: ${param.getDefaultParameterValue()?.getValue() ?: "Sin valor predeterminado"}"
    }?.join(', ') ?: "No hay parámetros"

    println "Nombre: ${job.name}"
    println "URL del proyecto: ${fullUrl}"
    println "URL del repositorio de GitLab: ${scmUrl}"
    println "Versión de Maven utilizada: ${mavenVersion}"
    println "Versión de JDK usada: ${jdk}"
    println "Última ejecución: ${job.getLastBuild().getTime()}"
    println "Últimos 5 ejecutores únicos: ${uniqueUsers.join(', ')}"
    println "Parámetros de la tarea: ${jobParameters}"
    println "-----------------------------------"
}
