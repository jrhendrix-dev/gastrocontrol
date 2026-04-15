#!/bin/bash
# Copies seed product images into the uploads volume on the server.
# Run once after first deploy or when adding new seed images.
# Usage: ./seed-images.sh [server_ip]

SERVER=${1:-144.126.232.55}
IMAGES_DIR="$(dirname "$0")/../src/main/resources/seed-images"
REMOTE_DIR="/var/lib/docker/volumes/gastrocontrol_gastro_uploads/_data/products"

echo "Creating remote directory..."
ssh root@$SERVER "mkdir -p $REMOTE_DIR"

echo "Copying images..."
scp "$IMAGES_DIR"/*.jpg "$IMAGES_DIR"/*.png "$IMAGES_DIR"/*.webp \
    root@$SERVER:$REMOTE_DIR/ 2>/dev/null || true

echo "Done. Setting permissions..."
ssh root@$SERVER "chmod 644 $REMOTE_DIR/*"