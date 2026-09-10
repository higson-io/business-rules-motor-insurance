package pl.decerto.higson.demo.apk.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import pl.decerto.higson.demo.apk.Need;
import pl.decerto.higson.demo.apk.Recommendation;

/**
 * Wynik analizy potrzeb na drucie — wspólny dla trzech linii.
 *
 * Niesie potrzeby razem z propozycją, bo razem są jedną analizą: co klient zgłosił i co z tego
 * wynika. Rozdzielone na dwa pola najwyższego poziomu kusiłyby, żeby pokazać jedno bez drugiego.
 *
 * ⛔ Trzecią listą były OSTRZEŻENIA (rek. 8.2.c). Zniknęły z ekranu 4 września 2026, a razem
 * z nimi stąd — pole, którego nikt nie renderuje, jest martwym ładunkiem na drucie.
 */
public class RecommendationDto {

	/** Kod wariantu albo `null`, gdy linia proponuje ZESTAW pokryć, a nie jeden wariant. */
	private String variantCode;
	private int coveredNeeds;
	private int totalNeeds;
	private List<String> missingCovers = new ArrayList<>();
	/** BUDGET_FIT / BUDGET_EXCEEDED / BUDGET_ABSENT — albo `null`, gdy reżim budżetu nie wymaga. */
	private String budgetOutcome;
	/**
	 * Tańszy wariant mieszczący się w oczekiwanej składce — ALTERNATYWA, nie podmiana.
	 * `variantCode` powyżej niesie wariant najlepiej dopasowany do potrzeb bez względu na cenę.
	 */
	private String alternativeVariantCode;
	private BigDecimal alternativePremium;
	/** Rek. 8.7. Puste, dopóki reżim nie wymaga uzasadnienia — i to też jest informacja. */
	private List<String> justification = new ArrayList<>();

	private List<NeedDto> needs = new ArrayList<>();

	public String getVariantCode() { return variantCode; }
	public void setVariantCode(String variantCode) { this.variantCode = variantCode; }
	public int getCoveredNeeds() { return coveredNeeds; }
	public void setCoveredNeeds(int coveredNeeds) { this.coveredNeeds = coveredNeeds; }
	public int getTotalNeeds() { return totalNeeds; }
	public void setTotalNeeds(int totalNeeds) { this.totalNeeds = totalNeeds; }
	public List<String> getMissingCovers() { return missingCovers; }
	public void setMissingCovers(List<String> missingCovers) { this.missingCovers = missingCovers; }
	public String getBudgetOutcome() { return budgetOutcome; }
	public void setBudgetOutcome(String budgetOutcome) { this.budgetOutcome = budgetOutcome; }
	public String getAlternativeVariantCode() { return alternativeVariantCode; }
	public void setAlternativeVariantCode(String code) { this.alternativeVariantCode = code; }
	public BigDecimal getAlternativePremium() { return alternativePremium; }
	public void setAlternativePremium(BigDecimal premium) { this.alternativePremium = premium; }
	public List<String> getJustification() { return justification; }
	public void setJustification(List<String> justification) { this.justification = justification; }
	public List<NeedDto> getNeeds() { return needs; }
	public void setNeeds(List<NeedDto> needs) { this.needs = needs; }

	public static RecommendationDto of(Recommendation source, List<Need> needs) {
		RecommendationDto dto = new RecommendationDto();
		dto.setVariantCode(source.getVariantCode());
		dto.setCoveredNeeds(source.getCoveredNeeds());
		dto.setTotalNeeds(source.getTotalNeeds());
		dto.setMissingCovers(source.getMissingCovers());
		dto.setBudgetOutcome(source.getBudgetOutcome());
		dto.setAlternativeVariantCode(source.getAlternativeVariantCode());
		dto.setAlternativePremium(source.getAlternativePremium());
		dto.setJustification(source.getJustification());
		for (Need need : needs) {
			dto.getNeeds().add(NeedDto.of(need));
		}
		return dto;
	}

	/** Potrzeba klienta i pokrycie, które ją zaspokaja. */
	public static class NeedDto {
		private String coverCode;
		private String text;
		private int weight;
		private boolean declared;

		public String getCoverCode() { return coverCode; }
		public void setCoverCode(String coverCode) { this.coverCode = coverCode; }
		public String getText() { return text; }
		public void setText(String text) { this.text = text; }
		public int getWeight() { return weight; }
		public void setWeight(int weight) { this.weight = weight; }
		public boolean isDeclared() { return declared; }
		public void setDeclared(boolean declared) { this.declared = declared; }

		static NeedDto of(Need need) {
			NeedDto dto = new NeedDto();
			dto.setCoverCode(need.getCoverCode());
			dto.setText(need.getText());
			dto.setWeight(need.getWeight());
			dto.setDeclared(need.isDeclared());
			return dto;
		}
	}

}
