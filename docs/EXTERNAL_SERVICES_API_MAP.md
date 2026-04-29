# External Services API Map

Last updated: 2026-04-29

This document maps non-Spring services present in the workspace and their API surface.

## 1) GEE Flask Service

Primary reference:
- [gee_app_with_cache_logic/README.md](../gee_app_with_cache_logic/README.md)

Runtime profile (from workspace docs/code):
- Default local port: `5000`
- Service role: image search/retrieval and Earth Engine processing support

Main endpoint families (as documented):
- `POST /api/gee/fetch-images`
- `POST /api/gee/process-image`
- `POST /api/gee/calculate-ndvi`
- `POST /api/gee/get-download-url`

Integration note:
- Backend geospatial modules proxy and normalize selected calls for frontend usage.

## 2) Image Processing FastAPI Service

Code entry points:
- [image_porcessing/app/REST_API_version2.py](../image_porcessing/app/REST_API_version2.py)
- [image_porcessing/app/REST_API_version1.py](../image_porcessing/app/REST_API_version1.py)
- [image_porcessing/app/main.py](../image_porcessing/app/main.py)

Detected routes from code:
- `POST /calculate/{index_type}` (version2)
- `GET /download/{filename}` (version2)
- `GET /` (version2/main)
- `POST /calculate-index/` (version1 legacy route)

Service role:
- Vegetation index computation and raster output generation.

Integration note:
- Backend resource/geospatial controllers should be considered the stable frontend-facing boundary.

## 3) RSIC Captioning API (FastAPI)

Reference:
- [rsic/README_API.md](../rsic/README_API.md)

Documented endpoints:
- `GET /health`
- `POST /caption/greedy`
- `POST /caption/beam`
- `POST /caption/compare`

Service role:
- Remote-sensing image caption generation (CNN+LSTM pipeline).

## 4) Urban Change Detection API

Reference:
- [urban-change-api/README_API.md](../urban-change-api/README_API.md)

Documented endpoints:
- `GET /`
- `GET /health`
- `POST /predict-change`

Service role:
- Change detection between two images; returns summary metrics and result file URLs.

## 5) Contract and Ownership Guidance

1. Prefer routing frontend integrations through Spring backend modules unless explicitly building a direct microservice client.
2. Keep external payload schemas versioned in docs if backend adapters start transforming fields.
3. Maintain timeout/retry policy consistency across backend adapters for long-running geospatial operations.
4. Add authentication expectations per service if direct frontend access is introduced.
