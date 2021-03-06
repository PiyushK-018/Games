#!/usr/bin/env groovy

@Library('ZomisJenkins')
import net.zomis.jenkins.Duga

pipeline {
    agent any

    stages {
        stage('Environment Vars') {
            steps {
                script {
                    sh 'rm -f .env.local'
                    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC'))
                    sh "echo 'VUE_APP_BUILD_TIME=$timestamp' >> .env.local"
                    sh "echo 'VUE_APP_BUILD_NUMBER=$env.BUILD_NUMBER' >> .env.local"
                    sh "echo 'VUE_APP_GIT_COMMIT=$env.GIT_COMMIT' >> .env.local"
                    sh "echo 'VUE_APP_GIT_BRANCH=$env.GIT_BRANCH' >> .env.local"
                    sh 'cat .env.local'
                    sh 'cp .env.local games-vue-client/'
                }
            }
        }
        stage('Build') {
            steps {
                sh 'cp /home/zomis/jenkins/server2-secrets.properties games-server/src/main/resources/secrets.properties'
                sh 'cp /home/zomis/jenkins/server2-startup.conf server2.conf.docker'
                sh './gradlew clean test :games-server:assemble :games-js:assemble'
                script {
                    def gitChanges = sh(script: 'git diff-index HEAD', returnStatus: true)
                    if (gitChanges) {
                        error("There are git changes after build")
                    }
                }
                sh 'cp games-js/.eslintrc.js games-js/web/'
                dir('games-vue-client') {
                    sh 'npm install && npm run build'
                }
            }
        }

        stage('Docker Image') {
            when {
                branch 'master'
            }
            steps {
                script {
                    // Stop running containers
                    sh 'docker ps -q --filter name="games_server" | xargs -r docker stop'
                    sh 'docker ps -q --filter name="games_client" | xargs -r docker stop'

                    // Deploy server
                    sh 'docker build . -t gamesserver2'
                    withCredentials([usernamePassword(
                          credentialsId: 'AWS_CREDENTIALS',
                          passwordVariable: 'AWS_SECRET_ACCESS_KEY',
                          usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
                        withEnv(["ENV_AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}", "ENV_AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}"]) {

                            def result = sh(script: """docker run -d --rm --name games_server -p 42638:42638 \
                              -e TZ=Europe/Amsterdam \
                              -e AWS_SECRET_ACCESS_KEY=$ENV_AWS_SECRET_ACCESS_KEY \
                              -e AWS_ACCESS_KEY_ID=$ENV_AWS_ACCESS_KEY_ID \
                              -v /etc/letsencrypt:/etc/letsencrypt \
                              -v /home/zomis/jenkins/gamesserver2:/data/logs \
                              -v /etc/localtime:/etc/localtime:ro \
                              -w /data/logs gamesserver2""",
                                returnStdout: true)
                            println(result)
                        }
                    }

                    // Deploy client
                    sh 'rm -rf /home/zomis/docker-volumes/games-vue-client'
                    sh 'cp -r $(pwd)/games-vue-client/dist /home/zomis/docker-volumes/games-vue-client'
                    sh 'docker run -d --rm --name games_client -v /home/zomis/docker-volumes/games-vue-client:/usr/share/nginx/html:ro -p 42637:80 nginx'
                }
            }
        }

/*
                withSonarQubeEnv('My SonarQube Server') {
                    // requires SonarQube Scanner for Maven 3.2+
                    sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar'
                }
*/
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/build/test-results/junit-platform/TEST-*.xml'
        }
        success {
            zpost(0)
        }
        unstable {
            zpost(1)
        }
        failure {
            zpost(2)
        }
    }
}
