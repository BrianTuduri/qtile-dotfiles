 import groovy.xml.XmlSlurper
 def slurper = new XmlSlurper().parseText(
    '<root><one a1="uno!"/><two>Some text!</two></root>' )


def propertiesContent = slurper.'**'.find { it.name() == 'propertiesContent' }

// Set the new value for propertiesContent
propertiesContent.replaceBody('PATH_DEFINED_PARAMETERS=Projects/uy-com-geocom-alkosto/geopos2-alkosto/parameters.groovy\nPATH_PARAMETERS=Projects/uy-com-geocom-alkosto/geopos2-alkosto/parameters.groovy')

// Print the modified XML
println XmlUtil.serialize(slurper)