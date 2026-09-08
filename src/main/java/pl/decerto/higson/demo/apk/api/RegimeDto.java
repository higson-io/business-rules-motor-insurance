package pl.decerto.higson.demo.apk.api;

/**
 * Reżim APK na drucie. Wspólny dla wszystkich trzech linii — front nie zna daty progu, dostaje
 * gotowy podpis i podstawę prawną, którą pokazuje ekran motoryzacyjny i życiowy.
 *
 * ⛔ Dwa przełączniki, nie cztery. `requiresExclusionCheck` i `requiresVariantDifferences`
 * odeszły 8 września 2026: pierwszy stracił czytelnika, gdy §25 zdjął blok wyłączeń ze wszystkich
 * trzech dem, drugi nie miał go nigdy. Przełącznik, którego nikt nie czyta, to nie konfiguracja
 * na przyszłość, tylko obietnica bez pokrycia. Zostały te dwa, które BRAMKUJĄ zachowanie:
 * `requiresBudget` (MOTO, LIFE) i `requiresJustification` (MOTO, LIFE).
 */
public class RegimeDto {

	private String code;
	private String name;
	private String legalBasis;
	private boolean requiresBudget;
	private boolean requiresJustification;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLegalBasis() {
		return legalBasis;
	}

	public void setLegalBasis(String legalBasis) {
		this.legalBasis = legalBasis;
	}

	public boolean isRequiresBudget() {
		return requiresBudget;
	}

	public void setRequiresBudget(boolean requiresBudget) {
		this.requiresBudget = requiresBudget;
	}

	public boolean isRequiresJustification() {
		return requiresJustification;
	}

	public void setRequiresJustification(boolean requiresJustification) {
		this.requiresJustification = requiresJustification;
	}

	/** Jedno miejsce, w którym reżim domenowy staje się reżimem na drucie. */
	public static RegimeDto of(pl.decerto.higson.demo.apk.Regime regime) {
		RegimeDto dto = new RegimeDto();
		dto.setCode(regime.getCode());
		dto.setName(regime.getName());
		dto.setLegalBasis(regime.getLegalBasis());
		dto.setRequiresBudget(regime.isRequiresBudget());
		dto.setRequiresJustification(regime.isRequiresJustification());
		return dto;
	}
}
