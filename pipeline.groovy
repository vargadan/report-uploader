node('maven') {
	def APP_NAME = "report-uploader"
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def CICD_PROJECT = "ctr-cicd"
   	def DEV_PROJECT = "ctr-dev"
   	def QA_PROJECT = "ctr-it"
   	def PROD_PROJECT = "ctr-prod"
   	def PORT = 8080
   	def GIT_URL = "https://github.com/vargadan/${APP_NAME}.git"
   	def SKIP_TEST = "false"
 
  	stage ('Build & Test') {
   		git branch: 'master', url: "${GIT_URL}"
   		sh "${mvnCmd} clean package -DskipTests=${SKIP_TEST} fabric8:build"
   	}
   	
   	def version = version()
   	
	stage ('Deploy DEV') {
	   	// create build. override the exit code since it complains about exising imagestream
	   	//tag for version in DEV imagestream
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:${version}"
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${DEV_PROJECT}/${APP_NAME}:latest"
		envSetup(DEV_PROJECT, APP_NAME, 'latest', PORT, true)
	}

   	stage ('Deploy to IT') {
        //put into IT imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${IT_PROJECT}/${APP_NAME}:latest"
        envSetup(IT_PROJECT, APP_NAME, 'latest', PORT, true)
	}
	
   	stage ('Deploy to PROD') {
        //put into PROD imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${PROD_PROJECT}/${APP_NAME}:latest"
        envSetup(PROD_PROJECT, APP_NAME, 'latest', PORT, false)
	}

}

def envSetup(project, appName, version, port, recreate) {
	GET_DC_OUT = sh (
		script: "oc get deploymentconfig -l app=${appName} -n ${project}",
		returnStdout: true
	).trim()
	echo "GET_DC_OUT : ${GET_DC_OUT}"
	appExists = GET_DC_OUT.contains(appName)
	if (appExists && !recreate) {
		//since the app exists and we dont recreate it we can exit
		return
	} else if (appExists && recreate) {
		//we can delete the app if we want to recreate
		sh "oc delete deploymentconfig,service,routes -l app=${appName} -n ${project}"
	}
	//now we can create the app since it has either been deleted or it did not exist at all
 	sh "oc new-app ${appName}:${version} -n ${project}"
   	sh "oc delete service,routes -l app=${appName} -n ${project}"
   	sh "oc create service clusterip dummy-report-factory --tcp=${port}:${port}"
   	sh "oc expose service dummy-report-factory"	
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}