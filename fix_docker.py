lines = open('docker-compose.yml').readlines()
new_lines = []
skip = False

skip_targets = ["mongodb:", "redis:", "rabbitmq:"]

i = 0
while i < len(lines):
    line = lines[i]
    stripped = line.strip()
    
    # Check if we should start skipping a service
    if line.startswith("  ") and not line.startswith("    ") and stripped in skip_targets:
        skip = True
        i += 1
        continue
    
    # Check if we are done skipping (another top-level service or section starts)
    if skip and (line.startswith("  ") and not line.startswith("    ") and stripped not in skip_targets and not line.startswith("#")):
        skip = False
    
    if skip:
        i += 1
        continue
    
    # Wait, the `# ===========================` might need to be kept or also skip might be false on them, that's fine.
    
    # Let's fix the commented `backend:`, `frontend:`, `prometheus:`, `grafana:` block (remove `# ` prefix)
    is_part_of_commented_block = line.startswith("  #   ") or line.startswith("  # - ")
    if is_part_of_commented_block:
        line = line.replace("  # ", "  ", 1)

    # Some blocks like `backend:` are already uncommented but properties are commented
    if line.startswith("  # ") and "condition: service" in line:
        line = line.replace("  # ", "  ", 1)

    # Remove depends_on mongodb, redis, rabbitmq
    if "mongodb:" in line and "condition" in (lines[i+1] if i+1 < len(lines) else ""):
        # It's a depends_on block, skip it and the next line
        i += 2
        continue
    if "redis:" in line and "condition" in (lines[i+1] if i+1 < len(lines) else ""):
        i += 2
        continue
    if "rabbitmq:" in line and "condition" in (lines[i+1] if i+1 < len(lines) else ""):
        i += 2
        continue
        
    # Same for prometheus depends_on
    if "- mongodb" in line or "- redis" in line or "- rabbitmq" in line:
        if "driver:" not in line:
            # check if it's a depends_on array item
            i+=1
            continue

    if stripped in ["mongodb_data:", "mongodb_config:", "redis_data:", "rabbitmq_data:"]:
        # Skip the volume and its next line (driver: local)
        i += 2
        continue

    # Remove local env vars from backend so they are picked up from environment or .env
    # We will just comment them out or remove them so it uses the real ones
    if "MONGO_URI: mongodb" in line or "REDIS_URL: redis" in line or "RABBITMQ_" in line:
        # let's just turn them into pass-through
        if "MONGO_URI:" in line: line = "      MONGO_URI: ${MONGO_URI}\n"
        if "REDIS_URL:" in line: line = "      REDIS_URL: ${REDIS_URL}\n"
        if "RABBITMQ_HOST:" in line: line = "      RABBITMQ_HOST: ${RABBITMQ_HOST}\n"
        if "RABBITMQ_PORT:" in line: line = "      RABBITMQ_PORT: ${RABBITMQ_PORT}\n"
        if "RABBITMQ_USER:" in line: line = "      RABBITMQ_USER: ${RABBITMQ_USER}\n"
        if "RABBITMQ_PASS:" in line: line = "      RABBITMQ_PASS: ${RABBITMQ_PASS}\n"

    # REDIS_HOST in gee-service:
    if "REDIS_HOST=redis" in line:
        line = "      - REDIS_HOST=${REDIS_HOST}\n"
    if "REDIS_PORT=6379" in line:
        line = "      - REDIS_PORT=${REDIS_PORT}\n"
    if "REDIS_PASSWORD=" in line:
        line = "      - REDIS_PASSWORD=${REDIS_PASSWORD}\n"

    new_lines.append(line)
    i += 1

open('docker-compose.yml', 'w').writelines(new_lines)
