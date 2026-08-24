package pl.decerto.higson.demo.apk.api;

/**
 * Reżim APK na drucie. Wspólny dla wszystkich trzech linii — front nie zna daty progu, dostaje
 * gotowy podpis, podstawę prawną i cztery przełączniki mówiące, czego reżim wymaga.
 */
public class RegimeDto {

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

	/** Jedno miejsce, w którym reżim domenowy staje się reżimem na drucie. */
	public static RegimeDto of(pl.decerto.higson.demo.apk.Regime regime) {
		RegimeDto dto = new RegimeDto();
		dto.setCode(regime.getCode());
		dto.setName(regime.getName());
		dto.setLegalBasis(regime.getLegalBasis());
		dto.setRequiresBudget(regime.isRequiresBudget());
		dto.setRequiresExclusionCheck(regime.isRequiresExclusionCheck());
		dto.setRequiresJustification(regime.isRequiresJustification());
		dto.setRequiresVariantDifferences(regime.isRequiresVariantDifferences());
		return dto;
	}
}
