void setBuildStatus(String message, String state) {
	step([
		$class: "GitHubCommitStatusSetter",
		reposSource: [$class: "ManuallyEnteredRepositorySource", url: env.GIT_URL],
		commitShaSource: [$class: "ManuallyEnteredShaSource", sha: env.GIT_COMMIT],
		contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
		errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
		statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
	]);
}

pipeline {
	agent any
	environment {
		GRADLE_OPTS='-Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2'
	}
	stages {
		stage('Notify GitHub') {
			steps {
				setBuildStatus('Build is pending', 'PENDING')
			}
		}
		stage('Prepare') {
			steps {
				sh 'chmod +x gradlew'
			}
		}
		stage('Build') {
			steps {
				sh './gradlew jpi -x test -x checkstyleMain'
			}
		}
		stage('Check code style') {
			steps {
				sh './gradlew checkstyleMain'
			}
			post {
				always {
					// Upload checkstyle report using Warnings Next Generation Plugin
					recordIssues enabledForFailure: true, aggregatingResults: true, tool: checkStyle(pattern: 'build/reports/checkstyle/*.xml')
				}
			}
		}
	}
	post {
		success {
			setBuildStatus('Build succeeded', 'SUCCESS')
		}
		failure {
			setBuildStatus('Build failed', 'FAILURE')
		}
		unstable {
			setBuildStatus('Build is unstable', 'UNSTABLE')
		}
	}
}