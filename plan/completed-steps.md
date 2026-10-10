# 🚀 Satellite Platform - Hands-On DevOps Guide & Runbook

This guide contains the exact step-by-step commands executed to take the Satellite Platform Application from bare source code to a hardened, zero-cost, local Kubernetes production architecture.

---

## 📑 Table of Contents
1. [Phase 1: Local Containerization & Health Verification](#phase-1-local-containerization--health-verification)
2. [Phase 2: Local Kubernetes (k3s) & Ingress Controller Setup](#phase-2-local-kubernetes-k3s--ingress-controller-setup)
3. [Phase 3: Database & Messaging Tier (MongoDB, Redis, RabbitMQ)](#phase-3-database--messaging-tier)
4. [Phase 4: Deploying Spring Boot Backend into k3s](#phase-4-deploying-spring-boot-backend-into-k3s)
5. [Debugging Playbook: Commands & Gotchas Encountered](#debugging-playbook-commands--gotchas-encountered)
6. [Cluster Health Check (Verify Everything)](#cluster-health-check-verify-everything)

---

## Phase 1: Local Containerization & Health Verification

### Step 1.1: Parameterize Environment Variables
Never commit credentials. Create your `.env` from the template:
```bash
cp .env.example .env
```
Ensure it is ignored by Git:
```bash
git status --ignored | grep .env || echo ".env" >> .gitignore
```

### Step 1.2: Compile the Spring Boot JAR
Compile the JAR on the host machine using Maven:
```bash
cd Backend
mvn clean package -DskipTests
cd ..
```

### Step 1.3: Build the Hardened Docker Image
Build the container with Eclipse Temurin JRE 17 Alpine and non-root user `spring`:
```bash
docker build -t satellite-backend:latest -f Backend/Dockerfile Backend/
```

### Step 1.4: Verify Full Stack Locally with Docker Compose
Start the health-gated stack:
```bash
docker compose up -d
```
Check that all containers are healthy:
```bash
docker compose ps
```
Verify Spring Boot Actuator:
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```
Stop the Compose stack to free ports (8080, 27017, 6379, 5672) for Kubernetes:
```bash
docker compose down
```

---

## Phase 2: Local Kubernetes (k3s) & Ingress Controller Setup

### Step 2.1: Install k3s with Traefik Disabled
Disable Traefik to use the industry-standard Ingress-NGINX:
```bash
sudo mkdir -p /etc/rancher/k3s
echo -e "disable:\n  - traefik" | sudo tee /etc/rancher/k3s/config.yaml
curl -sfL https://get.k3s.io | sh -
```

### Step 2.2: Configure Non-Root `kubectl` Access
Set up kubeconfig for your Linux user:
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
chmod 600 ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc
```
Verify the cluster node:
```bash
kubectl get nodes
# Expected: Ready
```

### Step 2.3: Install Helm 3 CLI
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### Step 2.4: Deploy Official Ingress-NGINX Controller
```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace
```
Verify the ingress controller pod and ports (80/443):
```bash
kubectl get pods,svc -n ingress-nginx
```

---

## Phase 3: Database & Messaging Tier

### Step 3.1: Create Application Namespace
```bash
kubectl create namespace satellite-platform
```

### Step 3.2: Deploy MongoDB via Bitnami Helm
Deploy MongoDB with 5Gi persistent storage on k3s `local-path`:
```bash
set -a; source .env; set +a
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm install mongodb bitnami/mongodb \
  --namespace satellite-platform \
  --set auth.rootPassword="${MONGO_ROOT_PASSWORD}" \
  --set auth.usernames[0]="${MONGO_ROOT_USER}" \
  --set auth.passwords[0]="${MONGO_ROOT_PASSWORD}" \
  --set auth.databases[0]="${MONGO_DATABASE}" \
  --set persistence.size=5Gi
```

### Step 3.3: Deploy Redis Standalone via Bitnami Helm
```bash
helm install redis bitnami/redis \
  --namespace satellite-platform \
  --set architecture=standalone \
  --set auth.enabled=false \
  --set master.persistence.size=2Gi
```

### Step 3.4: Deploy RabbitMQ via Native Kubernetes Manifest
*(Note: Bitnami Helm rabbitmq chart paywalled its Debian tags causing `Init:ErrImagePull`. We use a native StatefulSet manifest with `rabbitmq:3-management-alpine`)*

1. Delete any orphaned PVC if uninstalled from Bitnami:
   ```bash
   kubectl delete pvc data-rabbitmq-0 -n satellite-platform --ignore-not-found
   ```
2. Create the Secret from `.env`:
   ```bash
   set -a; source .env; set +a
   kubectl create secret generic rabbitmq-secret \
     --namespace satellite-platform \
     --from-literal=rabbitmq-user="${RABBITMQ_USER}" \
     --from-literal=rabbitmq-password="${RABBITMQ_PASS}" \
     --dry-run=client -o yaml | kubectl apply -f -
   ```
3. Apply the RabbitMQ manifest:
   ```bash
   kubectl apply -f k8s/infra/rabbitmq.yaml
   ```

### Step 3.5: Verify Infrastructure
```bash
kubectl get pods,pvc,svc -n satellite-platform
```
*Expected:* `mongodb`, `redis-master-0`, and `rabbitmq-0` are all `Running (1/1)` and their PVCs are `Bound`.

---

## Phase 4: Deploying Spring Boot Backend into k3s

### Step 4.1: Import the Local Docker Image into k3s Containerd
k3s uses containerd (`k3s ctr`), not the Docker daemon. Import the locally built image:
```bash
docker tag satellite_platform_application-backend:latest satellite-backend:latest
docker save satellite-backend:latest | sudo k3s ctr images import -
```

### Step 4.2: Create the Backend Kubernetes Secret
Dynamically generate the secret from `.env` (using `authSource=satellitedb` so MongoDB authenticates user `admin` on `satellitedb`):
```bash
set -a; source .env; set +a
kubectl create secret generic backend-secret \
  --namespace satellite-platform \
  --from-literal=MONGO_URI="mongodb://${MONGO_ROOT_USER}:${MONGO_ROOT_PASSWORD}@mongodb:27017/${MONGO_DATABASE}?authSource=${MONGO_DATABASE}" \
  --from-literal=RABBITMQ_USER="${RABBITMQ_USER}" \
  --from-literal=RABBITMQ_PASS="${RABBITMQ_PASS}" \
  --from-literal=JWT_SECRET="${JWT_SECRET}" \
  --from-literal=ADMIN_PASSWORD="${ADMIN_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -
```

### Step 4.3: Apply Backend Manifests
Deploy the PVC, ConfigMap, Deployment, Service, and Ingress:
```bash
kubectl apply -f k8s/backend/
```

### Step 4.4: Watch the Pod Rollout
```bash
kubectl get pods -n satellite-platform -w
```
*Expected:* Pod `satellite-backend-...` transitions to `Running (1/1)` with 0 restarts.

### Step 4.5: Test Public Access through Ingress-NGINX
Test via port 80 using the Host header:
```bash
curl -H "Host: api.satellite.localhost" http://localhost/actuator/health
```
*Expected Output:*
```json
{"status":"UP","groups":["liveness","readiness"]}
```

---

## Debugging Playbook: Commands & Gotchas Encountered

### 🛠️ The 3 Standard Commands to Debug Any Kubernetes Crash:

1. **View live container logs:**
   ```bash
   kubectl logs -l app=satellite-backend -n satellite-platform --tail=50
   ```
2. **View logs from the container that just crashed (`CrashLoopBackOff`):**
   ```bash
   kubectl logs -l app=satellite-backend -n satellite-platform --previous --tail=50
   ```
3. **View cluster lifecycle events, probes, and mount errors:**
   ```bash
   kubectl describe pod -l app=satellite-backend -n satellite-platform
   ```
4. **Trigger a clean zero-downtime rolling restart:**
   ```bash
   kubectl rollout restart deployment/satellite-backend -n satellite-platform
   ```

---

### ⚠️ Gotchas & How We Fixed Them:

#### Gotcha 1: `Permission denied: logs/app.log`
* **Log:** `java.io.FileNotFoundException: logs/app.log (Permission denied)`
* **Why:** Alpine created user `spring` with UID **100**, but Kubernetes `deployment.yaml` had `runAsUser: 1001`.
* **Fix Command:** Update `k8s/backend/deployment.yaml` with `runAsUser: 100`, `runAsGroup: 101`, and mount `emptyDir` scratch volumes at `/app/logs` and `/app/tmp`:
  ```bash
  kubectl apply -f k8s/backend/deployment.yaml
  ```

#### Gotcha 2: MongoDB `Authentication failed (Error 18)`
* **Log:** `com.mongodb.MongoCommandException: Command failed with error 18 (AuthenticationFailed)`
* **Why:** Bitnami created user `admin` in database `satellitedb`, but the connection string asked MongoDB to check system database `admin` (`authSource=admin`).
* **Fix Command:** Recreate the secret with `authSource=satellitedb`:
  ```bash
  kubectl create secret generic backend-secret \
    --namespace satellite-platform \
    --from-literal=MONGO_URI="mongodb://admin:admin123@mongodb:27017/satellitedb?authSource=satellitedb" \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl rollout restart deployment/satellite-backend -n satellite-platform
  ```

---

## Cluster Health Check (Verify Everything)

Run this one-liner to verify all workloads, volumes, and network ingress:
```bash
kubectl get pods,pvc,svc,ingress -n satellite-platform
```

You should see:
* `pod/mongodb-...` (1/1 Running)
* `pod/redis-master-0` (1/1 Running)
* `pod/rabbitmq-0` (1/1 Running)
* `pod/satellite-backend-...` (1/1 Running)
* 4 PVCs in `Bound` status
* `ingress/backend-ingress` routing `api.satellite.localhost`
