package pl.decerto.higson.demo.property.api.dto;

/** Reżim APK. Front nie wie, że data 2027-07-01 istnieje — dostaje gotowy podpis i przełączniki. */
public class PropertyRegimeDto {

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
