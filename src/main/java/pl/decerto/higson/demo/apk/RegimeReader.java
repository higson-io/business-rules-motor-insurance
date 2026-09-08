package pl.decerto.higson.demo.apk;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.engine.core.output.ParamValue;

/**
 * Czyta reżim APK z tabeli danej linii produktowej.
 *
 * ⛔ Wołany PO `engine.setEffectiveDate`, nigdy przed. Reżim jest wersjonowany datą polisy, więc
 * odczyt sprzed ustawienia daty zwróciłby wersję wybraną zegarem serwera — i wyglądałoby to na błąd
 * konfiguracji, a nie kodu.
 */
public final class RegimeReader {

	private RegimeReader() {
	}

	public static Regime read(HigsonEngine engine, String parameterCode, HigsonContext ctx) {
		ParamValue value = engine.get(parameterCode, ctx);
		Regime regime = new Regime();
		regime.setCode(value.getString("code"));
		regime.setName(value.getString("name"));
		regime.setLegalBasis(value.getString("legalBasis"));
		regime.setRequiresBudget(Boolean.TRUE.equals(value.getBoolean("requiresBudget")));
		regime.setRequiresJustification(Boolean.TRUE.equals(value.getBoolean("requiresJustification")));
		return regime;
	}
}
