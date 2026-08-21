package pl.decerto.higson.demo.property.domain;

/**
 * Reżim APK — parametr wersjonowany regionem. Druga połowa decyzji D-5: domena mówi, jakie pytania
 * w ogóle istnieją, a ten parametr mówi, co wolno z odpowiedziami zrobić.
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
