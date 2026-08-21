package pl.decerto.higson.demo.property.api.dto;

import java.util.List;

public class PropertyRecommendationDto {

	private String variantCode;
	private int coveredNeeds;
	private int totalNeeds;
	private List<String> missingCovers;
	private String budgetOutcome;
	private List<String> justification;

	public String getVariantCode() {
		return variantCode;
	}

	public void setVariantCode(String variantCode) {
		this.variantCode = variantCode;
	}

	public int getCoveredNeeds() {
		return coveredNeeds;
	}

	public void setCoveredNeeds(int coveredNeeds) {
		this.coveredNeeds = coveredNeeds;
	}

	public int getTotalNeeds() {
		return totalNeeds;
	}

	public void setTotalNeeds(int totalNeeds) {
		this.totalNeeds = totalNeeds;
	}

	public List<String> getMissingCovers() {
		return missingCovers;
	}

	public void setMissingCovers(List<String> missingCovers) {
		this.missingCovers = missingCovers;
	}

	public String getBudgetOutcome() {
		return budgetOutcome;
	}

	public void setBudgetOutcome(String budgetOutcome) {
		this.budgetOutcome = budgetOutcome;
	}

	public List<String> getJustification() {
		return justification;
	}

	public void setJustification(List<String> justification) {
		this.justification = justification;
	}
}
