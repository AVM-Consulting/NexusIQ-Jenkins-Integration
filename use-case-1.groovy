import hudson.model.*
import jenkins.model.Jenkins
import java.io.File;
import groovy.xml.XmlUtil
import static javax.xml.xpath.XPathConstants.*
import java.io.InputStream;
import java.io.FileInputStream
import javax.xml.transform.stream.StreamSource
import groovy.util.XmlParser

def startingPoints=["DevServices/Captial Markets and Finance/CMFT01/REO-VS/REOVS_RELEASE"]
def excludedPath = ["ZRelease"]

def allProject = []
def excludedProjects = []

Jenkins.instance.getAllItems(AbstractProject.class).each{item -> 
  def startingPoint = startingPoints.each{ start -> 
    if(item.fullName.toLowerCase().startsWith(start.toLowerCase())){
      allProject.add(item.fullName.toLowerCase())
    }
  }
}

Jenkins.instance.getAllItems(AbstractProject.class).each{item -> 
  def startingPoint = excludedPath.each{ start -> 
    if(item.fullName.toLowerCase().contains(start.toLowerCase())){
      excludedProjects.add(item.fullName.toLowerCase())
    }
  }
}

allProject.intersect(excludedProjects).each{allProject.remove(it);excludedProjects.remove(it)}

def includedProject= allProject+excludedProjects

def nexusIqBuilder = '''
<org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep plugin="nexus-jenkins-plugin@3.9.20200722-164144.e3a1be0">
<com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqStage>Build</com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqStage>
<com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqApplication class="org.sonatype.nexus.ci.iq.SelectedApplication">
<applicationId></applicationId>
</com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqApplication>
<com__sonatype__nexus__ci__iq__IqPolicyEvaluator____failBuildOnNetworkError>false</com__sonatype__nexus__ci__iq__IqPolicyEvaluator____failBuildOnNetworkError>
<com__sonatype__nexus__ci__iq__IqPolicyEvaluator____jobCredentialsId></com__sonatype__nexus__ci__iq__IqPolicyEvaluator____jobCredentialsId>
<advancedProperties></advancedProperties>
</org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep>

'''
Jenkins.instance.getAllItems(AbstractProject.class).each {item ->
  includedProject.each{include -> 
    if(item.fullName.toLowerCase() == include.toLowerCase()){
      def config = item.getConfigFile()
      File file = config.getFile()
      String fileContents = file.getText('UTF-8')
      xml=new XmlParser().parseText(fileContents)
      
      def nodeToModify=xml.builders.findAll{n-> 
          if(! n."org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep"){
              newBuilder = new XmlParser( false, true ).parseText( nexusIqBuilder )
              xml.find { it.name() == 'builders' }.children().add( 0, newBuilder )
              file.withWriter { w ->
                  w.write(XmlUtil.serialize(xml))
              }
              // reload jenkins job config file
              InputStream is = new FileInputStream(file)
              item.updateByXml(new StreamSource(is))
              item.save()
              }     
      }
      print("NexusIQ plugin added to "+ item.fullName +" in the build step\n")
    }
    else{
        print("NexusIQ plugin skipped in "+ item.fullName +"\n")
    }
  }
}

