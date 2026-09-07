# Odizolowany stos demo APK

Cztery kontenery — baza H2, Higson Studio, runtime-rest i aplikacja demo — postawione tak,
żeby **stanąć obok istniejących stosów na współdzielonym serwerze i niczego w nich nie ruszyć**.

## Na czym stoi izolacja

| Warstwa | Jak odseparowana |
|---|---|
| Projekt Compose | `name: ${STACK_NAME}` — `docker compose down` w cudzym katalogu nie sięga tych kontenerów |
| Nazwy kontenerów | prefiks `${STACK_NAME}-`, więc brak kolizji z `studio`, `db`, `demo` itd. |
| Porty | blok **58xxx**, z dala od domyślnych 8081 / 8082 / 38080 / 48080 |
| Sieć i wolumen | własne, prefiksowane nazwą projektu |

`install.sh` **sprawdza porty i nazwy kontenerów, zanim cokolwiek uruchomi**, i przerywa
z listą kolizji, nie zmieniwszy niczego.

## Złożenie paczki (na maszynie deweloperskiej)

```bash
mvn -o package -Pall_in_one -DskipTests     # najpierw WAR
deploy/isolated/package.sh /tmp/apk-demo-bundle
```

Powstaje `/tmp/apk-demo-bundle.tar.gz` (~110 MB) zawierająca:

- `images/app-image.tar` — obraz aplikacji zbudowany z bieżącego WAR-a (domyślnie `linux/amd64`;
  inna architektura: `PLATFORM=linux/arm64 deploy/isolated/package.sh`)
- `db/higson.demo.mv.db.gz` — **baza z całą konfiguracją trzech profili**, zrzucona
  z lokalnego kontenera
- `conf/` — `application.properties`, licencja Studia, poświadczenia Vertex AI

⚠️ **Paczka zawiera sekrety** (licencja, klucz JWT runtime'u, token integracyjny Studia).
Nie ma jej w repozytorium i nie może w nim wylądować — `.gitignore` blokuje `conf/` i `db/`.

## Wgranie na serwer

```bash
scp /tmp/apk-demo-bundle.tar.gz uzytkownik@serwer:~/
ssh uzytkownik@serwer 'tar -xzf apk-demo-bundle.tar.gz && cd apk-demo-bundle && ./install.sh'
```

`install.sh` wczytuje obraz, tworzy wolumen, **wgrywa do niego bazę zanim wstanie kontener**
(obraz seeduje własną, pustą bazę tylko do pustego wolumenu — odwrotna kolejność skasowałaby
całą konfigurację) i podnosi stos.

## Po instalacji

| Co | Adres |
|---|---|
| Aplikacja demo | `http://serwer:58090/demo` |
| Higson Studio | `http://serwer:58080/studio` |
| runtime-rest | `http://serwer:58081/runtime-rest` |
| Konsola H2 | `http://serwer:58082` |

Przesunięcie bloku portów: edycja `.env` przed `./install.sh`.

## Zatrzymanie i usunięcie

```bash
docker compose -p apk-demo down        # zatrzymuje, zostawia bazę
docker compose -p apk-demo down -v     # usuwa też wolumen z konfiguracją
```

Żadna z tych komend nie dotyka kontenerów spoza projektu `apk-demo`.
