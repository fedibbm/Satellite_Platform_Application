# Cloud-Native Deployment & CI/CD Plan (Option A)

This document outlines the step-by-step plan to migrate the Satellite Platform Application from a local Docker Compose setup to a professional, Cloud-Native architecture using Azure Container Apps and SaaS Free Tiers, orchestrated by GitHub Actions.

## Phase 1: Externalize Stateful Services (SaaS Migration)
**Goal:** Move databases and message brokers out of local Docker so they are accessible from anywhere.

- [ ] **1.1 MongoDB Atlas:*
  - Create a free cluster on MongoDB Atlas.
  - Whitelist IP addresses (allow all `0.0.0.0/0` for development, or specific Azure IPs later).
  - Update `.env.local` / `docker-compose.yml` to test the Atlas URI locally.
- [ ] **1.2 CloudAMQP (RabbitMQ):**
  - Create a free "Little Lemur" instance.
  - Update credentials in `.env` and test backend workflows locally against the cloud broker.
- [ ] **1.3 Upstash / Redis Cloud:**
  - Create a free Redis instance.
  - Update REDIS_URL in `.env` and verify caching/rate limiting works locally.

## Phase 2: Container Registry & Docker Optimization
**Goal:** Ensure images are built correctly and hosted on GitHub Container Registry (GHCR).

- [ ] **2.1 Prepare Dockerfiles:**
  - Verify Spring Boot, FastAPI/Flask, and Next.js Dockerfiles use multi-stage builds to minimize image size.
- [ ] **2.2 Create GHCR Personal Access Token (PAT):**
  - Generate a PAT in GitHub with `write:packages` scope.
- [ ] **2.3 GitHub Actions Workflow - Build & Push:**
  - Create `.github/workflows/docker-publish.yml`.
  - Configure triggers on `push` to `main`.
  - Add jobs to build and push the 4 containers (Backend, Frontend, GEE Service, Image Processing) to `ghcr.io`.

## Phase 3: Azure Infrastructure Provisioning (IaC)
**Goal:** Setup the Azure environment to host the containers using Azure CLI or Bicep/Terraform (we will use Azure CLI scripts for simplicity and learning).

- [ ] **3.1 Setup Azure Resource Group & Log Analytics:**
  - Create a Resource Group.
  - Create a Log Analytics workspace (required for Container Apps).
- [ ] **3.2 Setup Azure Container Apps Environment:**
  - Create the Environment bridging the apps together in a virtual network boundary.
- [ ] **3.3 Configure Identities:**
  - Create a Service Principal in Azure for GitHub Actions.
  - Save the Azure credentials to GitHub Secrets.

## Phase 4: Application Deployment & CI/CD Integration
**Goal:** Deploy the apps and automate updates on code push.

- [ ] **4.1 Deploy Python Services (GEE & Image Processing):**
  - Deploy via Azure CLI.
  - Setup environment variables (inject GEE Service Account JSON securely).
  - Verify endpoints.
- [ ] **4.2 Deploy Spring Boot Backend:**
  - Deploy via Azure CLI.
  - Inject MongoDB, RabbitMQ, Redis URIs, and Python internal URLs.
  - Verify `/actuator/health`.
- [ ] **4.3 Deploy Next.js Frontend:*
  - Build with the correct `NEXT_PUBLIC_API_BASE_URL` pointing to the Azure Backend URL.
  - Deploy container.
  - Setup CORS on Backend to allow the Azure Frontend URL.
- [ ] **4.4 Complete the CI/CD Pipeline:**
  - Update the GitHub Action to run `az containerapp update` after building a new image.

## Phase 5: Domain & Security (Bonus)
- [ ] Claim free GitHub Student domain.
- [ ] Map Custom Domain to the Frontend Container App.
- [ ] Ensure HTTPS is enforced everywhere.
