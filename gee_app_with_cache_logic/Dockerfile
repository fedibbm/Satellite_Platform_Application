# =============================================================================
# SATELLITE GEE SERVICE - PRODUCTION DOCKERFILE
# Stack: Python 3.12-slim, Flask, Uvicorn, Google Earth Engine
# =============================================================================

FROM python:3.12-slim

# Prevent Python from writing .pyc files and buffer stdout/stderr
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    DEBIAN_FRONTEND=noninteractive

WORKDIR /app

# Install minimal runtime dependencies (curl for container healthcheck)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Security: Create non-root user and group
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/bash -m appuser

# Layer caching: Install Python dependencies first
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source code with non-root ownership
COPY --chown=appuser:appgroup . .

# Create output and cache directories with proper permissions
RUN mkdir -p /app/output /app/cache && \
    chown -R appuser:appgroup /app/output /app/cache

# Switch to non-root execution
USER appuser

EXPOSE 5000

# Container healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

# Launch production ASGI server via Uvicorn
CMD ["uvicorn", "app:asgi_app", "--host", "0.0.0.0", "--port", "5000"]
