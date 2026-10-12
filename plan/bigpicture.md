# 🌐 Satellite Platform Application - End-to-End Architectural Blueprint

**Project:** Satellite Platform Application (`fedibbm/Satellite_Platform_Application`)  
**Deployment Target:** Local Production-Mirroring Kubernetes (`k3s`)  
**Cost:** $0.00 (Local-First Cloud Architecture)

---

## 1. System Topology & Network Mesh

```
                                  [ User Browser ]
                                         │
                         ┌───────────────┴───────────────┐
                         │    Ingress-NGINX (Port 80)    │
                         └───────────────┬───────────────┘
                                         │
             ┌───────────────────────────┴───────────────────────────┐
             │ Host: satellite.localhost                             │ Host: api.satellite.localhost
             ▼                                                       ▼
   ┌───────────────────┐                                   ┌───────────────────┐
   │ Frontend (Next.js)│ ─── (Client / Ingress API) ─────> │ Backend (Spring)  │
   │    Port 3000      │                                   │    Port 8080      │
   └───────────────────┘                                   └─────────┬─────────┘
                                                                     │
                         ┌───────────────────────────────────────────┼─────────────────────────────────┐
                         │                                           │                                 │
                         ▼                                           ▼                                 ▼
               ┌───────────────────┐                       ┌───────────────────┐             ┌───────────────────┐
               │    GEE Service    │                       │  Image Processor  │             │    Databases      │
               │   (Flask / GEE)   │                       │ (FastAPI / GDAL)  │             │ Mongo/Redis/Rabbit│
               │    Port 5000      │                       │    Port 8000      │             │  (Running in K8s) │
               └─────────┬─────────┘                       └───────────────────┘             └───────────────────┘
                         │
                         ▼
               (Google Earth Engine API)
```

---

## 2. Microservice Inventory & Port Mappings

| Service Name | Technology Stack | Container / Cluster Port | Exposed Via | Role in Architecture |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | Next.js 14, React 18, Tailwind | `3000` | Ingress: `satellite.localhost` | Web UI, satellite visualization dashboard, authentication UI. |
| **Backend** | Spring Boot 3, Java 17, Maven | `8080` | Ingress: `api.satellite.localhost` | Core business logic, JWT authentication, task orchestration, persistence. |
| **GEE Service** | Python 3.12, Flask, Uvicorn | `5000` | Internal Cluster DNS (`gee-service`) | Google Earth Engine data fetching, caching satellite tiles in Redis. |
| **Image Processor** | Python, FastAPI, GDAL, Rasterio | `8000` | Internal Cluster DNS (`image-processor`)| Multispectral analysis (NDVI, EVI, water detection), image manipulation. |
| **MongoDB** | MongoDB 7 (Bitnami StatefulSet) | `27017` | Cluster IP (`mongodb`) | Primary document store (`satellitedb`). |
| **Redis** | Redis 7 Alpine (Bitnami StatefulSet) | `6379` | Cluster IP (`redis-master`) | High-speed cache for GEE responses & token sessions. |
| **RabbitMQ** | RabbitMQ 3 Management Alpine | `5672` / `15672` | Cluster IP (`rabbitmq`) | Asynchronous event broker for long-running image processing jobs. |

---

## 3. DevOps Lifecycle & Automation Architecture

```
+---------------+     git push      +--------------------+     Webhook / Poll     +---------------+
|   Developer   | ----------------> |  GitHub Repository | ---------------------> |    Jenkins    |
+---------------+                   +--------------------+                        +---------------+
                                              |                                           |
                                              | Git is Single Source of Truth             | CI (Test, Build,
                                              | (k8s/ manifests)                          | Trivy Scan)
                                              v                                           v
                                    +--------------------+                       +-----------------+
                                    |       ArgoCD       |                       | Docker Registry |
                                    | (PULL Model in k8s)|                       | (Local Images)  |
                                    +--------------------+                       +-----------------+
                                              |
                                              | Continuous Reconciliation (Self-Healing)
                                              v
                                    +--------------------+
                                    |    k3s Cluster     |
                                    | (satellite-platform|
                                    +--------------------+
```

---

## 4. Phase Execution Roadmap

- [x] **Phase 1: Local Containerization & Health Verification**
  - Parameterized environment variables (`.env`).
  - Hardened Spring Boot backend (`Backend/Dockerfile`) with non-root user `spring`.
  - Health-gated multi-container stack in `docker-compose.yml`.
- [x] **Phase 2: Local Kubernetes (`k3s`) & Ingress-NGINX**
  - Installed lightweight k3s with Traefik disabled.
  - Installed Ingress-NGINX controller binding to host ports 80/443.
- [x] **Phase 3: Database & Messaging Tier**
  - Namespace `satellite-platform`.
  - MongoDB 7 (5Gi PVC, `satellitedb`).
  - Redis 7 (2Gi PVC, LRU caching).
  - RabbitMQ 3 (2Gi PVC, AMQP + Management UI).
- [x] **Phase 4: Spring Boot Backend Deployment into `k3s`**
  - Manifests in `k8s/backend/` (`pvc`, `configmap`, `secret`, `deployment`, `service`, `ingress`).
  - Resolved `CrashLoopBackOff` permission and MongoDB auth issues.
  - Ingress verified via `http://localhost/actuator/health` with Host `api.satellite.localhost`.
- [x] **Phase 5: Native Jenkins CI Pipeline with DevSecOps**
  - Installed native Jenkins on Ubuntu 24.04 as a systemd service.
  - Configured Docker group permissions and Java 21 / Maven / Docker runtimes.
  - Added Trivy filesystem and container image vulnerability scanners.
  - Created declarative `Jenkinsfile` with pre-flight checks, build, test, and security gates.
- [ ] **Phase 5.5: Full Microservices Containerization & Optimization [ACTIVE]**
  - Audit and optimize Dockerfiles:
    - `gee_app_with_cache_logic/Dockerfile` (Flask + GEE)
    - `image_porcessing/Dockerfile` (FastAPI + GDAL)
    - `FrontEnd/Dockerfile` (Next.js 14)
  - Create Kubernetes manifests for all microservices.
  - Wire inter-service cluster DNS communication.
- [ ] **Phase 6: GitOps Continuous Delivery with ArgoCD**
  - Install ArgoCD in `k3s` (`argocd` namespace).
  - Expose ArgoCD Web UI.
  - Create ArgoCD Application targeting `k8s/` manifests for automated reconciliation.
- [ ] **Phase 7: Full Stack Verification & Final Runbook**
  - Verify complete workflow from browser UI to Backend, GEE, and Image Processor.
