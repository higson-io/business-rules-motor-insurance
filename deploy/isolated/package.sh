#!/usr/bin/env bash
# Składa paczkę do wgrania na serwer: obraz aplikacji + baza z konfiguracją + pliki Studia.
# Uruchamiać z korzenia repozytorium, po `mvn package`.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-/tmp/apk-demo-bundle}"
DEMO_LOCAL="${DEMO_LOCAL:-$HOME/IdeaProjects/hyperon-devops/envs/demo-local}"
DB_CONTAINER="${DB_CONTAINER:-higson-demo-local-db}"
PLATFORM="${PLATFORM:-linux/amd64}"
IMAGE="${IMAGE:-decerto/higson-motor-insurance-demo:apk-demo}"

[ -f "$REPO/target/motor-insurance.war" ] || { echo "brak target/motor-insurance.war — uruchom najpierw mvn package -Pall_in_one" >&2; exit 1; }

rm -rf "$OUT"; mkdir -p "$OUT/images" "$OUT/db" "$OUT/conf"
cp "$REPO/deploy/isolated/docker-compose.yml" "$REPO/deploy/isolated/.env.example" "$REPO/deploy/isolated/install.sh" "$REPO/deploy/isolated/README.md" "$OUT/"

echo "==> buduję obraz aplikacji ($PLATFORM)"
docker buildx build --platform "$PLATFORM" -t "$IMAGE" --load "$REPO"
docker save "$IMAGE" -o "$OUT/images/app-image.tar"

echo "==> zrzucam bazę z konfiguracją"
# ⚠️ Baza jest kopiowana z DZIAŁAJĄCEGO kontenera. H2 trzyma plik otwarty, więc kopia bez
# zatrzymania zapisu może być niespójna — zatrzymujemy kontener na czas kopiowania.
docker stop "$DB_CONTAINER" >/dev/null
docker cp "$DB_CONTAINER:/h2-data/higson.demo.mv.db" "$OUT/db/higson.demo.mv.db"
docker start "$DB_CONTAINER" >/dev/null
gzip -9 "$OUT/db/higson.demo.mv.db"

echo "==> kopiuję konfigurację Studia"
# ⛔ Te trzy pliki NIE SĄ w repozytorium i nie mogą do niego trafić: licencja, klucz JWT
# runtime'u i token integracyjny Studia. Jadą wyłącznie w paczce, prosto na serwer.
for f in application.properties vertex-ai-credentials.json license; do
	cp "$DEMO_LOCAL/$f" "$OUT/conf/$f"
done
# W paczce baza jest widoczna pod nazwą usługi 'db', a nie 'higson-demo-db' jak lokalnie.
sed -i.bak 's#jdbc:h2:tcp://higson-demo-db:9092#jdbc:h2:tcp://db:9092#' "$OUT/conf/application.properties" && rm -f "$OUT/conf/application.properties.bak"

# Sekret JWT musi trafic do zmiennych srodowiskowych runtime-rest, a nie tylko do
# application.properties Studia - inaczej kontener runtime nie wstanie. .env powstaje tu,
# bo zawiera sekret i nie moze byc w repozytorium.
SECRET="$(grep -E '^higson\.security\.jwt\.secret-key=' "$OUT/conf/application.properties" | head -1 | cut -d= -f2-)"
[ -n "$SECRET" ] || { echo "nie znalazlem higson.security.jwt.secret-key w application.properties" >&2; exit 1; }
cp "$REPO/deploy/isolated/.env.example" "$OUT/.env"
printf 'JWT_SECRET=%s\n' "$SECRET" >> "$OUT/.env"

TAR="$OUT.tar.gz"
tar -czf "$TAR" -C "$(dirname "$OUT")" "$(basename "$OUT")"
echo
echo "Paczka: $TAR ($(du -h "$TAR" | cut -f1))"
echo "⚠️ Zawiera licencję, klucz JWT i token Studia — traktuj jak sekret."
