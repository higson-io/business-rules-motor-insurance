package pl.decerto;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Zabrania przeglądarce trzymać `index.html` bez pytania serwera.
 *
 * ⛔ POWSTAŁ Z PRAWDZIWEJ POMYŁKI, i to powtórzonej trzy razy w jednej sesji. `index.html` szedł
 * z samym `Last-Modified`, bez `Cache-Control`. Przeglądarka stosuje wtedy CACHE HEURYSTYCZNY —
 * przyjmuje własny czas ważności (zwykle ułamek wieku pliku) i przez ten czas serwuje kopię
 * z dysku, nie pytając serwera o nic. Skutek: po wdrożeniu zmiany ekran wyglądał na niezmieniony,
 * bo strona nadal ładowała POPRZEDNI, zahaszowany bundle — który leżał w artefakcie i odpowiadał
 * poprawnie. Wygląda to jak „zmiana nie doszła", a jest zwykłym cache'em.
 *
 * ⛔ Nagłówek dostaje TYLKO dokument HTML. Zahaszowane zasoby (`main-<HASH>.js`) są niezmienne
 * z definicji — nowa treść to nowa nazwa — więc ich cache'owanie jest właściwe i zostaje.
 * `no-cache` nie znaczy „nie zapisuj", tylko „przed użyciem zapytaj, czy się nie zmienił", więc
 * koszt to jedno warunkowe żądanie na wejście, nie ponowne pobranie aplikacji.
 *
 * ⚠️ Filtr, a nie konfiguracja zasobów Springa: `index.html` leży w korzeniu WAR-a i podaje go
 * domyślny serwlet Tomcata, którego `ResourceHandlerRegistry` nie obejmuje.
 *
 * ⚠️ Rejestrowany JAWNIE w `ApplicationConfig`, nie adnotacją `@Component`. Skan komponentów tej
 * aplikacji obejmuje wyłącznie trzy pakiety dem i nie sięga `pl.decerto` — filtr oznaczony
 * `@Component` po prostu nigdy by nie powstał, a nagłówka i tak by nie było.
 */
public class IndexNoCacheFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (request instanceof HttpServletRequest http && response instanceof HttpServletResponse out) {
			String path = http.getRequestURI();
			if (path.endsWith("/") || path.endsWith(".html")) {
				out.setHeader("Cache-Control", "no-cache, must-revalidate");
			}
		}
		chain.doFilter(request, response);
	}
}
