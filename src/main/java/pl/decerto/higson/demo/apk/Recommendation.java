package pl.decerto.higson.demo.apk;

import java.util.ArrayList;
import java.util.List;

/**
 * Wynik analizy potrzeb: wariant plus powody. Lista braków jest częścią odpowiedzi, a nie
 * przypisem — przekroczony budżet obniża propozycję, ale luka zostaje nazwana (decyzja D-7).
 */
public class Recommendation {

	private String variantCode;
	private int coveredNeeds;
	private int totalNeeds;
	private List<String> missingCovers = new ArrayList<>();
	private String budgetOutcome;
	private List<String> justification = new ArrayList<>();

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
