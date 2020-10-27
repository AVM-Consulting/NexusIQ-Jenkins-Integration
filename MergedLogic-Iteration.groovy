import hudson.model.*
import jenkins.model.Jenkins
import java.io.File;
import groovy.xml.XmlUtil
import java.io.InputStream;
import java.io.FileInputStream
import javax.xml.transform.stream.StreamSource
import com.cloudbees.hudson.plugins.folder.*

/*
  The starting folder path where processing begins
*/
STARTINGPATH = "DevServices/Captial Markets and Finance/CMFT01/REO-VS/REOVS_RELEASE"

/*
  The folder name pattern to exclude from the recursive processing
  NOTE: If this is empty, all the folders are skipped, so this should be set to '*' if all
  folders should be processed
*/
FOLDEREXCLUSIONPATTERN = "ZRelease"

/*
  Process those jobs that match this pattern. If this is empty, process all jobs, respecting the above 2 variables
*/
JOBINCLUSIONPATTERN = ""

STEPXML='''
<org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep plugin="nexus-jenkins-plugin@3.9.20200722-164144.e3a1be0">
  <com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqStage></com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqStage>
  <com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqApplication class="org.sonatype.nexus.ci.iq.SelectedApplication">
    <applicationId></applicationId>
  </com__sonatype__nexus__ci__iq__IqPolicyEvaluator____iqApplication>
  <com__sonatype__nexus__ci__iq__IqPolicyEvaluator____failBuildOnNetworkError>false</com__sonatype__nexus__ci__iq__IqPolicyEvaluator____failBuildOnNetworkError>
  <com__sonatype__nexus__ci__iq__IqPolicyEvaluator____jobCredentialsId></com__sonatype__nexus__ci__iq__IqPolicyEvaluator____jobCredentialsId>
  <advancedProperties></advancedProperties>
</org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep>
'''
/*
  Fetch the children of the node 'startingPath' and begin recursive processing
*/
Jenkins.getInstance().getItemByFullName(STARTINGPATH).each{
  if(it instanceof Folder){
    processFolder(it)
  } else {
    println("Skipping this object (" + it.name + ") as it is not a folder...")
  }
}

/*
  This function iterates through Folder objects recursively to process child Job objects
*/
void processFolder(Item folder){
  folder.getItems().each{
    if ( it instanceof Folder && !( it.name.contains(FOLDEREXCLUSIONPATTERN))){
    println("Processing folder: "+ it.name)
      processFolder(it)
    } else if ( it.name.contains(FOLDEREXCLUSIONPATTERN) ) {
      println("Skipping this Folder (" + it.name + ") as it is an exclusion...")
    } else {
      processJob(it)
    }
  }
}


/*
  This function is to process job objects. It reads the config XML, checks if the 'Invoke Nexus Policy Evaluation'
  step is present in the build steps and adds the step if it is not present. It then saves the XML and saves the Job
  so that the changes to the XML file are visible without needing a restart
*/
void processJob(Item job){
  if( job.name.contains(JOBINCLUSIONPATTERN)){
    println("Processing job: " + job.name)

    config = job.getConfigFile()
    File file = config.getFile()

    def root = new XmlSlurper().parse( file )
    fragmentToAdd = new XmlSlurper().parseText( STEPXML )

    if (! root.builders.childNodes().find{ it.name() == 'org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep'} ) {
      root.builders.appendNode( fragmentToAdd )
      def outxml = XmlUtil.serialize( root )

      file.withWriter { w ->
        w.write(XmlUtil.serialize(outxml))
      }

      InputStream is = new FileInputStream(file);
      job.updateByXml(new StreamSource(is));
      job.save();

      println("Added the build step and saved the job " + job.name)
    } else {
      println("Skipping insertion of build step as the step is present...")
    }
  } else {
    println ("Skipping the job '" + job.name + "' as it does not match the pattern")
  }
}