#!/bin/sh
# production database backup script for PostgreSQL 16
set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/var/backups/omnibooking}"
CONTAINER_NAME="${CONTAINER_NAME:-omnibooking-db-prod}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-omnibooking}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="omnibooking_prod_${TIMESTAMP}.sql.gz"
BACKUP_PATH="${BACKUP_DIR}/${FILENAME}"

echo "[$(date)] Initializing database backup..."

# Ensure backup directory exists
mkdir -p "${BACKUP_DIR}"

# Run pg_dump within the Docker container and pipe to gzip
echo "[$(date)] Running pg_dump on container '${CONTAINER_NAME}'..."
if ! docker exec -i "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" | gzip > "${BACKUP_PATH}"; then
  echo "ERROR: Backup failed! Check if container '${CONTAINER_NAME}' is running." >&2
  exit 1
fi

echo "[$(date)] Backup completed successfully: ${BACKUP_PATH}"
echo "[$(date)] Backup size: $(du -sh "${BACKUP_PATH}" | cut -f1)"

# retention cleanup
echo "[$(date)] Cleaning up local backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "omnibooking_prod_*.sql.gz" -type f -mtime +"${RETENTION_DAYS}" -delete
echo "[$(date)] Cleanup completed."
