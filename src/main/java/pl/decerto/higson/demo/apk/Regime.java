package pl.decerto.higson.demo.apk;

/**
 * Reżim analizy potrzeb klienta — co wolno i co trzeba zrobić z odpowiedziami.
 *
 * ⛔ Wspólny dla wszystkich linii produktowych i to nie jest oszczędność, tylko fakt prawny:
 * art. 8 ustawy o dystrybucji ubezpieczeń nie różnicuje ubezpieczeń majątkowych, komunikacyjnych
 * ani życiowych. Ten sam obowiązek, ten sam termin dostosowania z §2 uchwały KNF 190/2026, ten sam
 * kształt. Różni się wyłącznie katalog pytań — a ten jest konfiguracją.
 *
 * Wartości pochodzą z tabeli `demo.<linia>.apk.regime`, wersjonowanej regionem regulacyjnym.
 */
public class Regime {

	private String code;
	private String name;
	private String legalBasis;
	private boolean requiresBudget;
	private boolean requiresExclusionCheck;
	private boolean requiresJustification;
	private boolean requiresVariantDifferences;

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

	public boolean isRequiresExclusionCheck() {
		return requiresExclusionCheck;
	}

	public void setRequiresExclusionCheck(boolean requiresExclusionCheck) {
		this.requiresExclusionCheck = requiresExclusionCheck;
	}

	public boolean isRequiresJustification() {
		return requiresJustification;
	}

	public void setRequiresJustification(boolean requiresJustification) {
		this.requiresJustification = requiresJustification;
	}

	public boolean isRequiresVariantDifferences() {
		return requiresVariantDifferences;
	}

	public void setRequiresVariantDifferences(boolean requiresVariantDifferences) {
		this.requiresVariantDifferences = requiresVariantDifferences;
	}
}
