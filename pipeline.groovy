node('maven') {
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def DEV_PROJECT = "reportengine-dev"
   	def IT_PROJECT = "reportengine-it"
   	def PORT = 8080
   	def APP_NAME = "report-uploader"

   	stage ('Build') {
   		git branch: 'master', url: 'https://github.com/vargadan/report-uploader.git'
   		sh "${mvnCmd} clean install -DskipTests=true"
   	}
   	
   	def version = version()
   
 //  	stage ('Test SonarQube') {
 //  		sh "curl http://sonarqube:9000/batch/global"
 //  	}

   
   	stage ('Test and Analisys') {
   		parallel ( 
   			'Test' : { 
   				sh "${mvnCmd} test"
   				step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
   			},
   			'Static Ananlysis' : {
   				sh "${mvnCmd} org.jacoco:jacoco-maven-plugin:report sonar:sonar -Dsonar.host.url=http://sonarqube:9000/ -DskipTests=true"
   			}   			
   		)
   	}
   
 //  	stage ('Push to Nexus') {
 //  		sh "${mvnCmd} deploy -DskipTests=true"
 //  	}

   	stage ('Deploy DEV') {
	   // clean up. keep the image stream
	   sh "oc project ${DEV_PROJECT}"
	   sh "oc delete bc,dc,svc,route -l app=${APP_NAME} -n ${DEV_PROJECT}"
	   // create build. override the exit code since it complains about exising imagestream
	   sh "${mvnCmd} fabric8:deploy -DskipTests"
	}

   stage ('Deploy IT') {
     	timeout(time:5, unit:'MINUTES') {
        		input message: "Promote to IT?", ok: "Promote"
        }
	   sh "oc project ${IT_PROJECT}"
	   // tag for stage
	   sh "oc tag ${DEV_PROJECT}/${APP_NAME}:latest ${IT_PROJECT}/${APP_NAME}:${version}"
	   // clean up. keep the imagestream
	   sh "oc delete bc,dc,svc,route -l app=${APP_NAME} -n ${IT_PROJECT}"
	   // deploy stage image
	   sh "oc new-app ${APP_NAME}:${version} -n ${IT_PROJECT}" 
	   // delete service and route because new-app created them with wrong port
	   sh "oc delete svc,route -l app=${APP_NAME} -n ${IT_PROJECT}"
	   // create service with the right port 
	   sh "oc expose dc ${APP_NAME} --port=${PORT}"
	   // create route with the right port
	   sh "oc expose svc ${APP_NAME}"
	}
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
