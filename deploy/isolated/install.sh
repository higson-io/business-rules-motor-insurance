#!/usr/bin/env bash
# Instalacja odizolowanego stosu na serwerze. Uruchamiać z katalogu paczki.
set -euo pipefail

cd "$(dirname "$0")"
[ -f .env ] || cp .env.example .env
set -a; . ./.env; set +a
STACK="${STACK_NAME:-apk-demo}"

# ⛔ NAJPIERW sprawdź porty. Compose przy zajętym porcie po prostu nie wstaje, a komunikat
# ("address already in use") nie mówi, KTÓRY stos już go trzyma — a na współdzielonym
# serwerze to jedyne pytanie, które ma znaczenie.
busy=""
for p in "$PORT_STUDIO" "$PORT_RUNTIME" "$PORT_DB_CONSOLE" "$PORT_DB_TCP" "$PORT_WEB"; do
	if docker ps --format '{{.Names}} {{.Ports}}' | grep -qE "(^|[^0-9:]):${p}->"; then
		busy="$busy $p($(docker ps --format '{{.Names}} {{.Ports}}' | grep -E ":${p}->" | cut -d' ' -f1 | tr '\n' ','))"
	elif command -v ss >/dev/null && ss -ltn 2>/dev/null | grep -qE ":${p}\b"; then
		busy="$busy $p(poza dockerem)"
	fi
done
if [ -n "$busy" ]; then
	echo "PRZERWANE — te porty są zajęte:$busy" >&2
	echo "Przesuń blok w .env i uruchom ponownie. Nic nie zostało zmienione." >&2
	exit 1
fi

# Nazwa kontenera też musi być wolna — inaczej Compose zabiłby cudzy kontener przy 'up'.
for svc in db studio runtime-rest web; do
	if docker ps -a --format '{{.Names}}' | grep -qx "${STACK}-${svc}"; then
		echo "PRZERWANE — kontener ${STACK}-${svc} już istnieje. Zmień STACK_NAME w .env." >&2
		exit 1
	fi
done

echo "==> wczytuję obraz aplikacji"
docker load -i images/app-image.tar

echo "==> tworzę wolumen i wgrywam bazę z konfiguracją"
docker volume create "${STACK}_h2-data" >/dev/null
# Baza musi wylądować w wolumenie ZANIM wstanie kontener bazy — obraz seeduje pustą bazę
# tylko wtedy, gdy wolumen jest pusty, więc kolejność jest tu jedyną rzeczą, która o tym decyduje.
docker run --rm -v "${STACK}_h2-data":/h2-data -v "$PWD/db":/src alpine \
	sh -c 'gunzip -c /src/higson.demo.mv.db.gz > /h2-data/higson.demo.mv.db'

echo "==> startuję stos"
docker compose up -d

echo
echo "Gotowe. Stos '${STACK}':"
echo "  aplikacja  http://$(hostname -I 2>/dev/null | awk '{print $1}'):${PORT_WEB}${APP_CONTEXT_PATH}"
echo "  Studio     http://$(hostname -I 2>/dev/null | awk '{print $1}'):${PORT_STUDIO}/studio"
echo "  runtime    http://$(hostname -I 2>/dev/null | awk '{print $1}'):${PORT_RUNTIME}/runtime-rest"
echo
echo "Zatrzymanie:  docker compose -p ${STACK} down"
echo "Usunięcie z bazą:  docker compose -p ${STACK} down -v"
