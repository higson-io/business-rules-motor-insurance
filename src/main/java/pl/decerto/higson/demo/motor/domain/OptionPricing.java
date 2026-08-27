package pl.decerto.higson.demo.motor.domain;

import java.math.BigDecimal;

/**
 * Cena wariantu: suma składek pokryć minus zniżki.
 *
 * ⛔ JEDNO miejsce, w którym ta arytmetyka istnieje. Liczyły ją dotąd wyłącznie wnętrzności
 * konwertera, a od kiedy rekomendacja porównuje cenę z budżetem klienta, potrzebuje jej także
 * kontekst reguł. Druga kopia oznaczałaby, że ekran składek i rekomendacja mogą kiedyś podać dwie
 * różne ceny tego samego wariantu — i że nikt się o tym nie dowie.
 *
 * Same składki i zniżki pochodzą z Higsona; tutaj jest tylko ich dodawanie.
 */
public final class OptionPricing {

	private OptionPricing() {
	}

	/**
	 * ⚠️ Pomija pokrycia bez policzonej składki, zamiast się na nich wywracać.
	 *
	 * Kontekst reguł powstaje ZANIM wycena przypisze składki — reżim APK czyta się na początku
	 * `Motor.calculate`, więc w tym momencie `Coverage.getPremium()` jest jeszcze `null`.
	 * Wcześniej ta arytmetyka żyła wyłącznie w konwerterze, który biegnie po wycenie i nigdy tego
	 * stanu nie widział; `reduce` na `null` rzucał NPE dopiero po jej wyjęciu tutaj.
	 */
	public static BigDecimal beforeDiscounts(Option option) {
		BigDecimal sum = BigDecimal.ZERO;
		for (Coverage coverage : option.getCoverages()) {
			if (coverage.getPremium() != null) {
				sum = sum.add(coverage.getPremium());
			}
		}
		return sum;
	}

	public static BigDecimal finalPremium(Option option) {
		BigDecimal premium = beforeDiscounts(option);
		for (Discount discount : option.getDiscounts()) {
			if (discount.getValue() != null) {
				premium = premium.subtract(discount.getValue());
			}
		}
		return premium;
	}
}
