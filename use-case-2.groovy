import hudson.model.*
import jenkins.model.Jenkins
import java.io.File;
import groovy.xml.XmlUtil
import static javax.xml.xpath.XPathConstants.*
import java.io.InputStream;
import java.io.FileInputStream
import javax.xml.transform.stream.StreamSource
import groovy.util.XmlParser

def job_suffix = "_build"
def startingPoints= ["Most_Granular_Level_Of_Jenkins_Folder"]
def includedPath = ["Project_1", "Project_2"]

def includedProjects = []

Jenkins.instance.getAllItems(AbstractProject.class).each{item -> 
  if(item.fullName.toLowerCase().endsWith(job_suffix.toLowerCase())){
    def startingPoint = includedPath.each{ start -> 
        if(item.fullName.toLowerCase().contains(start.toLowerCase())){
            includedProjects.add(item.fullName.toLowerCase())
        }
    }
  }
}

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
  includedProjects.each{include -> 
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

