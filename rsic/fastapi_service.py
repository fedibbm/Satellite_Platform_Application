"""
Dummy FastAPI service for RSIC image captioning.
Preserves the same endpoint contracts as the original service.
"""

from __future__ import annotations

import hashlib
from typing import Dict

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
import uvicorn


app = FastAPI(
    title="Image Captioning API",
    description="Dummy CNN+LSTM-compatible API for generating captions from remote sensing images",
    version="1.0.0-dummy",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _image_signature(image_bytes: bytes) -> str:
    return hashlib.sha256(image_bytes).hexdigest()[:8]


def _dummy_caption(sig: str, mode: str) -> str:
    captions = [
        f"urban area with dense buildings and road network ({sig})",
        f"mixed vegetation and built-up zone near transport corridor ({sig})",
        f"residential blocks with sparse green patches and open spaces ({sig})",
        f"industrial-like structures adjacent to bare soil and paved surfaces ({sig})",
    ]
    base = captions[int(sig[0], 16) % len(captions)]
    if mode == "beam_search":
        return f"high-confidence: {base}"
    return base


@app.get("/", tags=["Info"])
async def root() -> Dict:
    return {
        "name": "Image Captioning API",
        "description": "Dummy CNN+LSTM service for remote sensing image captioning",
        "version": "1.0.0-dummy",
        "status": "ready",
        "model": "CNN+LSTM (Dummy)",
        "endpoints": {
            "/caption/greedy": "POST - Generate caption using greedy decoding",
            "/caption/beam": "POST - Generate caption using beam search",
            "/caption/compare": "POST - Compare greedy and beam outputs",
            "/health": "GET - Health check",
            "/docs": "GET - API documentation",
        },
    }


@app.get("/health", tags=["Info"])
async def health_check() -> Dict:
    return {
        "status": "healthy",
        "device": "cpu",
        "model_loaded": True,
        "vocab_loaded": True,
        "mode": "dummy",
    }


@app.post("/caption/greedy", tags=["Caption Generation"])
async def generate_caption_greedy_endpoint(file: UploadFile = File(...)) -> Dict:
    if not file:
        raise HTTPException(status_code=400, detail="File is required")

    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Uploaded file is empty")

    sig = _image_signature(contents)
    caption = _dummy_caption(sig, "greedy")

    return {
        "caption": caption,
        "method": "greedy",
        "image_size": [224, 224],
        "model": "CNN+LSTM",
        "success": True,
    }


@app.post("/caption/beam", tags=["Caption Generation"])
async def generate_caption_beam_endpoint(
    file: UploadFile = File(...),
    beam_width: int = 5,
) -> Dict:
    if beam_width < 1 or beam_width > 20:
        raise HTTPException(status_code=400, detail="Beam width must be between 1 and 20")

    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Uploaded file is empty")

    sig = _image_signature(contents)
    caption = _dummy_caption(sig, "beam_search")

    return {
        "caption": caption,
        "method": "beam_search",
        "beam_width": beam_width,
        "image_size": [224, 224],
        "model": "CNN+LSTM",
        "success": True,
    }


@app.post("/caption/compare", tags=["Caption Generation"])
async def compare_methods(file: UploadFile = File(...)) -> Dict:
    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Uploaded file is empty")

    sig = _image_signature(contents)

    return {
        "image_size": [224, 224],
        "captions": {
            "greedy": _dummy_caption(sig, "greedy"),
            "beam_search": _dummy_caption(sig, "beam_search"),
        },
        "model": "CNN+LSTM",
        "success": True,
    }


if __name__ == "__main__":
    uvicorn.run(
        "fastapi_service:app",
        host="0.0.0.0",
        port=8010,
        reload=False,
        log_level="info",
    )
