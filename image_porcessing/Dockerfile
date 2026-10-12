# =============================================================================
# SATELLITE IMAGE PROCESSING SERVICE - PRODUCTION DOCKERFILE
# Stack: Python 3.12-slim, FastAPI, GDAL, Rasterio, Uvicorn
# =============================================================================

FROM python:3.12-slim

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Install GDAL system dependencies and curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    gdal-bin \
    libgdal-dev \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set GDAL C/C++ include headers for Python bindings
ENV CPLUS_INCLUDE_PATH=/usr/include/gdal \
    C_INCLUDE_PATH=/usr/include/gdal

WORKDIR /app

# Security: Create non-root user and group
RUN groupadd -g 10002 appgroup && \
    useradd -u 10002 -g appgroup -s /bin/bash -m appuser

# Install Python dependencies first for optimal Docker layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files and grant permissions to appuser
COPY --chown=appuser:appgroup ./app /app
RUN mkdir -p /app/output && chown -R appuser:appgroup /app/output

# Switch to non-root execution
USER appuser

EXPOSE 8000

# Container healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:8000/ || exit 1

# Launch FastAPI ASGI server
CMD ["uvicorn", "REST_API_version2:app", "--host", "0.0.0.0", "--port", "8000"]
