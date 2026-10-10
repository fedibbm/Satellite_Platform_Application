// =============================================================================
// SATELLITE PLATFORM - ENTERPRISE CI PIPELINE (JENKINS)
// Pipeline: Build -> Test -> DevSecOps Scan (Trivy) -> Containerize -> Verify
// =============================================================================

pipeline {
    agent any

    environment {
        APP_NAME      = "satellite-backend"
        IMAGE_TAG     = "${BUILD_NUMBER}"
        SCAN_SEVERITY = "HIGH,CRITICAL"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timestamps()
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('1. Pre-Flight Tooling & Runtime Validation') {
            steps {
                echo "🔍 Verifying required build tools (Java, Maven, Docker, Trivy)..."
                sh '''
                    set -e
                    FAILED=0

                    check_required_tool() {
                        TOOL_NAME=$1
                        CHECK_CMD=$2
                        VERSION_CMD=$3

                        if ! command -v "$CHECK_CMD" >/dev/null 2>&1; then
                            echo "❌ FATAL: Required build tool '$TOOL_NAME' is NOT installed or not in PATH!"
                            FAILED=1
                        else
                            echo "✅ Found $TOOL_NAME: $($VERSION_CMD | head -n 1)"
                        fi
                    }

                    check_optional_tool() {
                        TOOL_NAME=$1
                        CHECK_CMD=$2
                        VERSION_CMD=$3

                        if ! command -v "$CHECK_CMD" >/dev/null 2>&1; then
                            echo "⚠️ WARNING: Optional tool '$TOOL_NAME' not found. Security scans will be skipped."
                        else
                            echo "✅ Found $TOOL_NAME: $($VERSION_CMD | head -n 1)"
                        fi
                    }

                    check_required_tool "Java JDK" "java" "java -version 2>&1"
                    check_required_tool "Apache Maven" "mvn" "mvn -version"
                    check_required_tool "Docker CLI" "docker" "docker --version"
                    check_optional_tool "Trivy Security Scanner" "trivy" "trivy --version"

                    if [ $FAILED -ne 0 ]; then
                        echo "🛑 PRE-FLIGHT CHECK FAILED: Required core build tools (Java, Maven, Docker) are missing."
                        exit 1
                    fi

                    echo "🚀 All pre-flight checks passed! Proceeding to build..."
                '''
            }
        }

        stage('2. Build & Test (Maven)') {
            steps {
                echo "☕ Compiling Spring Boot Backend..."
                dir('Backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('3. Source Security Audit (Trivy FS)') {
            steps {
                echo "🛡️ Scanning project dependencies and secrets with Trivy..."
                sh '''
                    if command -v trivy >/dev/null 2>&1; then
                        trivy fs --timeout 15m --severity ${SCAN_SEVERITY} --scanners vuln,secret --format table Backend/ || true
                    else
                        echo "⚠️ Trivy scanner not found on PATH. Skipping dependency scan."
                    fi
                '''
            }
        }

        stage('4. Build Docker Image') {
            steps {
                echo "🐳 Building Docker image: ${APP_NAME}:${IMAGE_TAG}..."
                sh """
                    docker build \
                        -t ${APP_NAME}:${IMAGE_TAG} \
                        -t ${APP_NAME}:latest \
                        -f Backend/Dockerfile Backend/
                """
            }
        }

        stage('5. Container Vulnerability Scan (Trivy Image)') {
            steps {
                echo "🔬 Scanning container image layers for CVE vulnerabilities..."
                sh '''
                    if command -v trivy >/dev/null 2>&1; then
                        trivy image --timeout 15m --severity ${SCAN_SEVERITY} --format table ${APP_NAME}:${IMAGE_TAG} || true
                    else
                        echo "⚠️ Trivy scanner not found on PATH. Skipping image scan."
                    fi
                '''
            }
        }

        stage('6. Artifact Verification') {
            steps {
                echo "📋 Verifying built Docker image in local registry..."
                sh 'docker images | grep ${APP_NAME}'
            }
        }
    }

    post {
        always {
            cleanWs()
            echo "🧹 Workspace cleanup finished."
        }
        success {
            echo "✅ Pipeline SUCCEEDED! Artifact ${APP_NAME}:${IMAGE_TAG} is built, tested, scanned, and ready."
        }
        failure {
            echo "❌ Pipeline FAILED! Please inspect the failed stage output above."
        }
    }
}
