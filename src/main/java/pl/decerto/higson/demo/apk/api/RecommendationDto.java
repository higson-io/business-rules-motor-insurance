package pl.decerto.higson.demo.apk.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import pl.decerto.higson.demo.apk.Need;
import pl.decerto.higson.demo.apk.Recommendation;
import pl.decerto.higson.demo.apk.Warning;

/**
 * Wynik analizy potrzeb na drucie — wspólny dla trzech linii.
 *
 * Niesie trzy listy razem, bo razem są jedną analizą: co klient zgłosił, co z tym kłóci się
 * w OWU i co z tego wynika. Rozdzielone na trzy pola najwyższego poziomu kusiłyby, żeby
 * pokazać jedno bez drugiego.
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
	private List<WarningDto> warnings = new ArrayList<>();

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
	public List<WarningDto> getWarnings() { return warnings; }
	public void setWarnings(List<WarningDto> warnings) { this.warnings = warnings; }

	public static RecommendationDto of(Recommendation source, List<Need> needs, List<Warning> warnings) {
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
		for (Warning warning : warnings) {
			dto.getWarnings().add(WarningDto.of(warning));
		}
		return dto;
	}

	/** Potrzeba klienta i pokrycie, które ją zaspokaja. */
	public static class NeedDto {
		private String coverCode;
		private String text;
		private int weight;

		public String getCoverCode() { return coverCode; }
		public void setCoverCode(String coverCode) { this.coverCode = coverCode; }
		public String getText() { return text; }
		public void setText(String text) { this.text = text; }
		public int getWeight() { return weight; }
		public void setWeight(int weight) { this.weight = weight; }

		static NeedDto of(Need need) {
			NeedDto dto = new NeedDto();
			dto.setCoverCode(need.getCoverCode());
			dto.setText(need.getText());
			dto.setWeight(need.getWeight());
			return dto;
		}
	}

	/** Wyłączenie, które styka się z sytuacją klienta. `remedy` jedzie razem — patrz rek. 8.2.c. */
	public static class WarningDto {
		private String code;
		private int severity;
		private String text;
		private String remedy;

		public String getCode() { return code; }
		public void setCode(String code) { this.code = code; }
		public int getSeverity() { return severity; }
		public void setSeverity(int severity) { this.severity = severity; }
		public String getText() { return text; }
		public void setText(String text) { this.text = text; }
		public String getRemedy() { return remedy; }
		public void setRemedy(String remedy) { this.remedy = remedy; }

		static WarningDto of(Warning warning) {
			WarningDto dto = new WarningDto();
			dto.setCode(warning.getCode());
			dto.setSeverity(warning.getSeverity());
			dto.setText(warning.getText());
			dto.setRemedy(warning.getRemedy());
			return dto;
		}
	}
}
