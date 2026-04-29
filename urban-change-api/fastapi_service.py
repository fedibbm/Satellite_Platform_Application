"""
Dummy FastAPI service for urban change detection.
Preserves input/output contract from the hosted API docs.
"""

from __future__ import annotations

import hashlib
import uuid
from pathlib import Path
from typing import Dict

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
import uvicorn


app = FastAPI(
    title="Urban Change Detection API",
    description="Dummy API for bi-temporal urban change detection",
    version="1.0.0-dummy",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

PLACEHOLDER_DIR = Path(__file__).parent / "placeholders"
PLACEHOLDER_FILES = {
    "overlay": "overlay_pred.png",
    "mask": "mask_final.png",
    "panel": "panel.png",
    "prob": "prob.png",
    "npz": "outputs.npz",
}


def _ratio_from_pair(a_bytes: bytes, b_bytes: bytes) -> float:
    digest = hashlib.sha256(a_bytes + b_bytes).hexdigest()
    value = int(digest[:8], 16)
    return round((value % 2500) / 10000.0 + 0.01, 4)  # 0.01 .. 0.26


@app.get("/")
async def root() -> Dict:
    return {
        "name": "Urban Change Detection API",
        "status": "ready",
        "mode": "dummy",
        "endpoints": {
            "/": "GET",
            "/health": "GET",
            "/predict-change": "POST",
        },
    }


@app.get("/health")
async def health() -> Dict:
    missing = [
        filename for filename in PLACEHOLDER_FILES.values()
        if not (PLACEHOLDER_DIR / filename).exists()
    ]
    return {
        "status": "healthy" if not missing else "degraded",
        "mode": "dummy",
        "model_loaded": True,
        "missing_placeholders": missing,
    }


@app.post("/predict-change")
async def predict_change(
    request: Request,
    image_a: UploadFile = File(...),
    image_b: UploadFile = File(...),
) -> Dict:
    a_bytes = await image_a.read()
    b_bytes = await image_b.read()

    if not a_bytes or not b_bytes:
        raise HTTPException(status_code=400, detail="Both image_a and image_b are required and must be non-empty")

    request_id = str(uuid.uuid4())
    ratio = _ratio_from_pair(a_bytes, b_bytes)
    prob_mean = round(max(ratio - 0.02, 0.001), 4)
    global_prob_mean = round(max(prob_mean - 0.01, 0.001), 4)

    base = str(request.base_url).rstrip("/")

    return {
        "request_id": request_id,
        "message": "Inference completed",
        "summary": {
            "patch_id": request_id,
            "status": "ok",
            "height": 1024,
            "width": 1024,
            "pred_ratio_final": ratio,
            "prob_mean": prob_mean,
            "global_prob_mean": global_prob_mean,
        },
        "files": {
            "overlay_pred": f"{base}/result/{request_id}/overlay",
            "mask_final": f"{base}/result/{request_id}/mask",
            "panel": f"{base}/result/{request_id}/panel",
            "prob": f"{base}/result/{request_id}/prob",
            "outputs_npz": f"{base}/result/{request_id}/npz",
        },
    }


@app.get("/result/{request_id}/overlay")
async def result_overlay(request_id: str):
    file_path = PLACEHOLDER_DIR / PLACEHOLDER_FILES["overlay"]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Missing placeholder file: {file_path.name}")
    return FileResponse(file_path, media_type="image/png", filename=f"overlay_{request_id}.png")


@app.get("/result/{request_id}/mask")
async def result_mask(request_id: str):
    file_path = PLACEHOLDER_DIR / PLACEHOLDER_FILES["mask"]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Missing placeholder file: {file_path.name}")
    return FileResponse(file_path, media_type="image/png", filename=f"mask_{request_id}.png")


@app.get("/result/{request_id}/panel")
async def result_panel(request_id: str):
    file_path = PLACEHOLDER_DIR / PLACEHOLDER_FILES["panel"]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Missing placeholder file: {file_path.name}")
    return FileResponse(file_path, media_type="image/png", filename=f"panel_{request_id}.png")


@app.get("/result/{request_id}/prob")
async def result_prob(request_id: str):
    file_path = PLACEHOLDER_DIR / PLACEHOLDER_FILES["prob"]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Missing placeholder file: {file_path.name}")
    return FileResponse(file_path, media_type="image/png", filename=f"prob_{request_id}.png")


@app.get("/result/{request_id}/npz")
async def result_npz(request_id: str):
    file_path = PLACEHOLDER_DIR / PLACEHOLDER_FILES["npz"]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Missing placeholder file: {file_path.name}")
    return FileResponse(file_path, media_type="application/octet-stream", filename=f"outputs_{request_id}.npz")


if __name__ == "__main__":
    uvicorn.run(
        "fastapi_service:app",
        host="0.0.0.0",
        port=8020,
        reload=False,
        log_level="info",
    )
