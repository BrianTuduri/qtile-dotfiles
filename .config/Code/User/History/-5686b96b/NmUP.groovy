 import groovy.xml.XmlSlurper
 def rootNode = new XmlSlurper().parseText(
    '<root><one a1="uno!"/><two>Some text!</two></root>' )

 assert rootNode.name() == 'root'
 assert rootNode.one[0].@a1 == 'uno!'
 assert rootNode.two.text() == 'Some text!'
 rootNode.children().each { assert it.name() in ['one','two'] }
 

Note that in some cases, a 'selector' expression may not resolve to a single node. For example:

 import groovy.xml.XmlSlurper
 def rootNode = new XmlSlurper().parseText(
    '''<root>
         <a>one!</a>
         <a>two!</a>
       </root>''' )

 assert rootNode.a.size() == 2
 rootNode.a.each { assert it.text() in ['one!','two!'] }