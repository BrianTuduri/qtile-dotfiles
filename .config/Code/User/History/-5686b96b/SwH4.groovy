import groovy.xml.XmlUtil

def xmlString = '''<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@1400.v7fd111b_ec82f">
  <!-- XML Content Omitted for Brevity -->
  <properties>
    <EnvInjectJobProperty plugin="envinject@2.908.v66a_774b_31d93">
      <info>
        <propertiesContent></propertiesContent>
        <secureGroovyScript plugin="script-security@1335.vf07d9ce377a_e">
          <script></script>
          <sandbox>false</sandbox>
        </secureGroovyScript>
        <loadFilesFromMaster>false</loadFilesFromMaster>
      </info>
      <on>true</on>
      <keepJenkinsSystemVariables>true</keepJenkinsSystemVariables>
      <keepBuildVariables>true</keepBuildVariables>
      <overrideBuildParameters>false</overrideBuildParameters>
    </EnvInjectJobProperty>
  </properties>
  <!-- More XML Content Omitted -->
</flow-definition>'''

def slurper = new XmlSlurper().parseText(xmlString)
def propertiesContent = slurper.'**'.find { it.name() == 'propertiesContent' }

// Set the new value for propertiesContent
propertiesContent.replaceBody('PATH_DEFINED_PARAMETERS=Projects/uy-com-geocom-alkosto/geopos2-alkosto/parameters.groovy\nPATH_PARAMETERS=Projects/uy-com-geocom-alkosto/geopos2-alkosto/parameters.groovy')

// Print the modified XML
println XmlUtil.serialize(slurper)
