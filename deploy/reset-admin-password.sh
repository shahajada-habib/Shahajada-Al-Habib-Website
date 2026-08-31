#!/usr/bin/env bash
# One-shot admin password reset — writes a fresh bcrypt hash straight into the
# shared database. No app restart, no seeder, no editing .env.
#
#   cd ~/Shahajada-Al-Habib-Website/deploy
#   bash reset-admin-password.sh 'YourNewPassword'
#
# Note: the database is shared with Render, so this changes the admin password
# there too (same account).
set -euo pipefail
cd "$(dirname "$0")"

NEWPASS="${1:-}"
if [ "${#NEWPASS}" -lt 8 ]; then
  echo "Usage: bash reset-admin-password.sh '<new password, at least 8 characters>'" >&2
  exit 1
fi

[ -f .env ] || { echo "ERROR: .env not found in $(pwd)" >&2; exit 1; }
set -a; . ./.env; set +a
: "${DB_URL:?missing from .env}"
: "${DB_USERNAME:?missing from .env}"
: "${DB_PASSWORD:?missing from .env}"

echo "Generating bcrypt hash..."
HASH=$(docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" "$NEWPASS" | tr -d ':\n' | sed 's/^\$2y/\$2a/')

HOSTPORT=$(printf '%s' "$DB_URL" | sed -E 's#^jdbc:mysql://([^/?]+).*#\1#')
DBNAME=$(printf '%s' "$DB_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^/?]+).*#\1#')
DBHOST=${HOSTPORT%:*}
DBPORT=${HOSTPORT#*:}

echo "Updating admin in ${DBHOST}:${DBPORT}/${DBNAME} ..."
docker run --rm mysql:8.0 mysql \
  --host="$DBHOST" --port="$DBPORT" --user="$DB_USERNAME" --password="$DB_PASSWORD" \
  --ssl-mode=REQUIRED "$DBNAME" \
  -e "UPDATE users SET password='$HASH', status='active' WHERE username='admin';
      SELECT username, role, status, LEFT(password, 7) AS hash_prefix FROM users;"

echo
echo "Done. Log in at  <your-host>/admin/  with:"
echo "   username: admin"
echo "   password: $NEWPASS"
echo "Then change it from the admin Settings screen."
