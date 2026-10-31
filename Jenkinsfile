// Jenkinsfile — declarative pipeline for the portfolio-valuation demo app.
//
// Flow:  Checkout -> Build & Verify -> Docker Build -> (main only) Push to ECR -> Deploy to ECS
//
// This is designed for a *Multibranch Pipeline* job:
//   * every branch and every pull request (job name "PR-<n>") runs the first three stages,
//     so a PR proves that tests pass AND the Docker image builds;
//   * only the "main" branch pushes an image and deploys it to the AWS sandbox.
//
// AWS access: Jenkins runs on an EC2 instance with an IAM *instance profile role*
// (policy in infra/jenkins-iam-policy.json). The AWS CLI picks the role up from the
// instance metadata service automatically, so no access keys are stored in Jenkins
// and nothing is hard-coded here. The account id is looked up with STS at run time.
//
// Remaining Jenkins credentials (Manage Jenkins -> Credentials), both optional:
//   anthropic-api-key  : Secret text (only for the AI triage hook in the post block)
//   github-token       : Secret text (used by the triage script to comment on PRs)

pipeline {
  // The local Jenkins image (see jenkins/Dockerfile) already has JDK 21, Maven,
  // the Docker CLI and the AWS CLI, so any agent will do.
  agent any

  options {
    timestamps()                                    // prefix every log line with a time
    disableConcurrentBuilds()                       // one build per branch at a time
    buildDiscarder(logRotator(numToKeepStr: '20'))  // keep disk usage under control
  }

  environment {
    AWS_REGION  = 'eu-west-2'
    APP_NAME    = 'portfolio-valuation'
    ECR_REPO    = 'portfolio-valuation'             // created by infra/terraform/ecr.tf
    ECS_CLUSTER = 'portfolio-valuation-cluster'     // created by infra/terraform/ecs.tf
    ECS_SERVICE = 'portfolio-valuation-service'     // created by infra/terraform/ecs.tf
    // IMAGE_TAG (short git SHA, e.g. "a1b2c3d") and AWS_ACCOUNT_ID are set from
    // script in the stages below. Declarative 'environment' values cannot be
    // overridden from script, so they deliberately are not declared here.
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
        script {
          // Every build gets a unique, traceable image tag; "latest" is an alias.
          env.IMAGE_TAG = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          echo "Building ${env.APP_NAME} @ ${env.IMAGE_TAG} (branch: ${env.BRANCH_NAME})"
        }
      }
    }

    stage('Build & Verify') {
      steps {
        // "verify" runs: compile -> unit tests -> Spotless lint -> JaCoCo coverage gate -> package.
        // If any of those fail the build goes red and nothing is pushed/deployed.
        sh 'mvn -B verify'
      }
      post {
        always {
          // Publish test results (shows the "Test Result" trend on the job page).
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
          // Publish JaCoCo coverage (JaCoCo plugin step).
          jacoco execPattern: 'target/jacoco.exec',
                 classPattern: 'target/classes',
                 sourcePattern: 'src/main/java'
          // Keep the jar as a build artifact for download/inspection.
          archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
        }
      }
    }

    stage('Docker Build') {
      // Runs on every branch and PR: a PR that breaks the Dockerfile fails here.
      steps {
        sh """
          docker build \
            --platform linux/amd64 \
            --tag ${APP_NAME}:${IMAGE_TAG} \
            --tag ${APP_NAME}:latest \
            .
        """
      }
    }

    stage('Push to ECR') {
      when { branch 'main' }
      steps {
        script {
          // No keys needed: the CLI uses the EC2 instance role. This call also
          // proves the role is attached before we try to push anything.
          env.AWS_ACCOUNT_ID = sh(
              script: "aws sts get-caller-identity --query Account --output text --region ${AWS_REGION}",
              returnStdout: true).trim()
          echo "Deploying with the instance role in account ${env.AWS_ACCOUNT_ID}"
        }
        sh '''#!/usr/bin/env bash
            set -euo pipefail
            ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
            ECR_IMAGE="${ECR_REGISTRY}/${ECR_REPO}"

            # Log in to ECR (the password is a short-lived token).
            aws ecr get-login-password --region "${AWS_REGION}" \
              | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

            # Tag the local image with the full ECR name and push both tags.
            docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_IMAGE}:${IMAGE_TAG}"
            docker tag "${APP_NAME}:${IMAGE_TAG}" "${ECR_IMAGE}:latest"
            docker push "${ECR_IMAGE}:${IMAGE_TAG}"
            docker push "${ECR_IMAGE}:latest"
            echo "Pushed ${ECR_IMAGE}:${IMAGE_TAG}"
        '''
      }
    }

    stage('Deploy to ECS') {
      when { branch 'main' }
      steps {
        sh '''#!/usr/bin/env bash
            set -euo pipefail
            ECR_IMAGE="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:${IMAGE_TAG}"

            # 1. Find the task definition the service is currently running.
            CURRENT_TD=$(aws ecs describe-services --region "${AWS_REGION}" \
                --cluster "${ECS_CLUSTER}" --services "${ECS_SERVICE}" \
                --query 'services[0].taskDefinition' --output text)
            echo "Current task definition: ${CURRENT_TD}"

            # 2. Copy it, swap in the new image, and strip the read-only fields that
            #    register-task-definition refuses to accept.
            aws ecs describe-task-definition --region "${AWS_REGION}" \
                --task-definition "${CURRENT_TD}" \
                --query 'taskDefinition' \
              | jq --arg IMG "${ECR_IMAGE}" '
                  .containerDefinitions[0].image = $IMG
                  | del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
                        .compatibilities, .registeredAt, .registeredBy)' \
              > new-task-def.json

            # 3. Register the copy as a new revision.
            NEW_TD=$(aws ecs register-task-definition --region "${AWS_REGION}" \
                --cli-input-json file://new-task-def.json \
                --query 'taskDefinition.taskDefinitionArn' --output text)
            echo "Registered: ${NEW_TD}"

            # 4. Point the service at the new revision (rolling deployment) and wait
            #    until the new task is healthy behind the load balancer.
            aws ecs update-service --region "${AWS_REGION}" \
                --cluster "${ECS_CLUSTER}" --service "${ECS_SERVICE}" \
                --task-definition "${NEW_TD}" > /dev/null
            echo "Waiting for the service to stabilise (this can take 2-3 minutes)..."
            aws ecs wait services-stable --region "${AWS_REGION}" \
                --cluster "${ECS_CLUSTER}" --services "${ECS_SERVICE}"

            # 5. Print the public URL.  The ALB DNS name is looked up by tag so we
            #    don't need to hard-code it (Terraform tags it Name=<app>-alb).
            ALB_DNS=$(aws elbv2 describe-load-balancers --region "${AWS_REGION}" \
                --query "LoadBalancers[?LoadBalancerName=='${APP_NAME}-alb'].DNSName" \
                --output text)
            echo "=================================================================="
            echo "  Deployed ${IMAGE_TAG} -> http://${ALB_DNS}/actuator/health"
            echo "=================================================================="
        '''
      }
    }
  }

  post {
    success {
      echo "SUCCESS: ${env.APP_NAME} ${env.IMAGE_TAG} on ${env.BRANCH_NAME}"
    }
    failure {
      echo "FAILURE: ${env.APP_NAME} ${env.IMAGE_TAG} on ${env.BRANCH_NAME} — see the console log above"
      script {
        // ------------------------------------------------------------------
        // Optional: AI-assisted failure triage for pull requests.
        //
        // scripts/triage_agent.py reads the console log, asks Claude what went
        // wrong, and posts a comment on the PR.  It only runs when:
        //   * this is a PR build (CHANGE_ID is set by the GitHub Branch Source plugin), and
        //   * the 'anthropic-api-key' and 'github-token' credentials exist.
        // If either credential is missing the step is skipped rather than failing.
        // ------------------------------------------------------------------
        if (env.CHANGE_ID) {
          try {
            withCredentials([string(credentialsId: 'anthropic-api-key', variable: 'ANTHROPIC_API_KEY'),
                             string(credentialsId: 'github-token',      variable: 'GITHUB_TOKEN')]) {
              // The repo slug ("owner/name") is derived from the checkout URL.
              def repoSlug = sh(script: "git config --get remote.origin.url | sed -E 's#(git@|https://)github.com[:/]##; s#\\.git\$##'",
                                returnStdout: true).trim()
              // Capture this build's console log so far and hand it to the script.
              // (Alternative: pass --build-url ${BUILD_URL} and let the script fetch it.)
              sh "curl -sf ${env.BUILD_URL}consoleText > build.log || true"
              sh """
                python3 scripts/triage_agent.py \
                  --log-file build.log \
                  --repo '${repoSlug}' \
                  --pr '${env.CHANGE_ID}'
              """
            }
          } catch (err) {
            echo "Triage agent skipped (credentials missing or script failed): ${err}"
          }
        }
      }
    }
    always {
      // Remove dangling images so the Jenkins host doesn't fill up over a term's worth of demos.
      sh 'docker image prune -f > /dev/null 2>&1 || true'
    }
  }
}
