node('maven') {
	def APP_NAME = "report-uploader"
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def CICD_PROJECT = "ctr-cicd"
   	def DEV_PROJECT = "ctr-dev"
   	def IT_PROJECT = "ctr-it"
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
	}

   	stage ('Deploy to IT') {
        //put into IT imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${IT_PROJECT}/${APP_NAME}:latest"
	}
	
   	stage ('Deploy to PROD') {
        //put into PROD imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${PROD_PROJECT}/${APP_NAME}:latest"
	}

}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}