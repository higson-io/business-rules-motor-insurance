package pl.decerto.higson.demo.apk;

/**
 * Reżim analizy potrzeb klienta — co wolno i co trzeba zrobić z odpowiedziami.
 *
 * ⛔ Wspólny dla wszystkich linii produktowych i to nie jest oszczędność, tylko fakt prawny:
 * art. 8 ustawy o dystrybucji ubezpieczeń nie różnicuje ubezpieczeń majątkowych, komunikacyjnych
 * ani życiowych. Ten sam obowiązek, ten sam termin dostosowania z §2 uchwały KNF 190/2026, ten sam
 * kształt. Różni się wyłącznie katalog pytań — a ten jest konfiguracją.
 *
 * Wartości pochodzą z tabeli `demo.<linia>.apk.regime`. W MOTO i LIFE jest ona wersjonowana
 * regionem regulacyjnym; w PROPERTY nie — tam obowiązuje jeden reżim (D-17).
 *
 * ⚠️ Tabele MOTO i LIFE mają jeszcze kolumny `requiresExclusionCheck`
 * i `requiresVariantDifferences`, których od 8 września 2026 nikt nie czyta. Zostały tam, bo obie
 * tabele są wersjonowane regionem, a edycja obu wersji niesie ryzyko nieproporcjonalne do zysku.
 * W PROPERTY wszystkie cztery kolumny są usunięte — tam reżim jest jeden i niesie już tylko kod,
 * którym kluczują się warunki oferty.
 */
public class Regime {

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
}
