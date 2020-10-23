# NexusIQ-Jenkins-Integration

## This script helps update multiple Jenkins jobs at once. It enables NexusIQ plugin post build step in Jenkins job. 

### Use case 1:

> use-case-1.groovy file correspond to the following use-case, where we want the integration to be part of all the jobs (after the build step) in all the folders except Project_3_Job_1

```sh
Project_1
	Project_1_Job_1
	Project_1_Job_2
Project_2
	Project_2_Job_1
	Project_2_Job_2
Project_3
	Project_3_Job_1
	Project_3_Job_2
``` 

#### Key variables for use-case 1:
- startingPoints: The path of the Jenkins job until the specific folder level from which the script will iterate and scan for the specified conditions to match.
- excludedPath: The project(s) we wish to exclude from executing.

************************************************************************
### Use case 2:

> use-case-2.groovy file correspond to the following use-case, where we want the integration to be part of specific jobs (with prefix _build_) inside specific folder(s) only. 

```sh
Project_1
	Project_1_Job_1_Build
	Project_1_Job_2
Project_2
	Project_2_Job_1_Build
	Project_2_Job_2
Project_3
	Project_3_Job_3_Build
	Project_3_Job_3	
``` 

#### Key variables for use-case 2:
- job_suffix: Specifies conditions for the job name. 
- startingPoints:  The path of the Jenkins job until the specific folder level from which the script will iterate and scan for the specified conditions to match.
- includedPath: The project(s) we wish to include from executing.


Reference - https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins