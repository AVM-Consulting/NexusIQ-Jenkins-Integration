import hudson.model.*
import jenkins.model.Jenkins
import java.io.File;
import groovy.xml.XmlUtil
import java.io.InputStream;
import java.io.FileInputStream
import javax.xml.transform.stream.StreamSource
import com.cloudbees.hudson.plugins.folder.*

/*
* User defined Variables:
* =======================
* STARTING_PATH: The starting folder path where processing begins
*
* *The following 2 FOLDER_ variables are mutually exclusive. If there are values in both (or if both are empty), the script will not process any folder*
* FOLDER_EXCLUSIONS: The list of folders to exclude from the recursive processing
* FOLDER_INCLUSIONS: The list of folders to be processed. Subfolders will also be processed
*
* JOB_INCLUSION_PATTERN: Process those jobs that match this pattern. If this is empty, process all jobs, respecting the above 2 variables
*
* STEPXML: XML obtained from a valid job that corresponds to the Build step
*/
STARTING_PATH = "A/Specific/Jenkins/Starting/Path"

FOLDER_EXCLUSIONS = ["Exclued_Folder_1","Exclued_Folder_2"]
FOLDER_INCLUSIONS = []

JOB_INCLUSION_PATTERN = "_build"

STEPXML='''
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
NODE_TO_ADD = new XmlSlurper().parseText( STEPXML )


/*
* If both lists have values, exit.
* If not, fetch the children of the node 'STARTING_PATH' and begin recursive processing
*/
if ( FOLDER_EXCLUSIONS.size() > 0 && FOLDER_INCLUSIONS.size() > 0 ) {
  println("Enter values for either of the lists and rerun. Exiting...")
} else if ( FOLDER_EXCLUSIONS.size() == 0 && FOLDER_INCLUSIONS.size() == 0 ){
  println("Both the lists are empty, enter values for either of the lists and rerun. Exiting...")
} else {
  Jenkins.getInstance().getItemByFullName(STARTING_PATH).each{
    if(it instanceof Folder){
      processFolder(it)
    } else {
      println("Skipping this object (" + it.name + ") as it is not a folder...")
    }
  }
}

/*
* This function iterates through Folder objects recursively to process children Jobs and folders, logic below
* If the variable FOLDER_EXCLUSIONS has values, it excludes those folders and processes all other folder (including children)
* in the path provided in STARTING_PATH
*
* If the variable FOLDER_INCLUSIONS has values, it processes the jobs in it and any subfolders contained in levels below the
* mentioned values recursively. Any folder or job that is not a descendent of the folders provided in this variable are excluded
*/
void processFolder(Item folder){
  folder.getItems().each{
    if (it instanceof Folder){
      if(FOLDER_EXCLUSIONS.size() > 0){
        if(it.name in (FOLDER_EXCLUSIONS)) {
          println("Skipping this Folder (" + it.name + ") as it is an exclusion...")
        } else {
          println("Processing folder: "+ it.name)
          processFolder(it)
        }
      } else if(FOLDER_INCLUSIONS.size() > 0){
        if(it.name in (FOLDER_INCLUSIONS)) {
          println("Processing folder: "+ it.name)
          processFolder(it)
        } else {
          def pathArray = it.fullName.tokenize('/')
          if(FOLDER_INCLUSIONS.intersect(pathArray).size() > 0){
            println("Processing child folder: "+ it.name)
            processFolder(it)
          } else {
            println ("Skipping this Folder (" + it.name + ") as it is not a child of included folders...")
          }
        }
      }
    } else {
      processJob(it)
    }
  }
}


/*
* This function is to process job objects. It reads the config XML, checks if the 'Invoke Nexus Policy Evaluation'
* step is present in the build steps and adds the step if it is not present. It then saves the XML and saves the Job
* so that the changes to the XML file are visible without needing a service restart
*/
void processJob(Item job){
  if( job.name.contains(JOB_INCLUSION_PATTERN)){
    println("Processing job: " + job.name)

    config = job.getConfigFile()
    File file = config.getFile()

    def root = new XmlSlurper().parse( file )

    if (! root.builders.childNodes().find{ it.name() == 'org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep'} ) {
      root.builders.appendNode( NODE_TO_ADD )
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
