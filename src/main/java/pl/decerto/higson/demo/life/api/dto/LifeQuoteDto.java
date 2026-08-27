package pl.decerto.higson.demo.life.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import pl.decerto.higson.demo.apk.api.QuestionDto;
import pl.decerto.higson.demo.apk.api.RecommendationDto;
import pl.decerto.higson.demo.apk.api.RegimeDto;
import java.time.LocalDate;
import java.util.List;

/**
 * Kontrakt wyceny życiowej.
 *
 * binding jest osobnym polem, a nie brakiem premium — inaczej front nie odróżni ceny wiążącej
 * od wstępnej i pokaże składkę REFER tak samo jak ACCEPT (D-1).
 */
public class LifeQuoteDto {

	private String lineCode;

	private LocalDate startDate;
	private BigDecimal mainSum;
	private String paymentFrequency;

	private LocalDate birthDate;
	private Integer entryAge;
	private boolean smoker;
	private int heightCm;
	private int weightKg;
	private BigDecimal bmi;
	private int occupationClass;
	private BigDecimal monthlyIncome;
	private LocalDate priorCoverFrom;
	private LocalDate priorCoverTo;

	private String decision;
	private boolean binding;
	private String message;
	private boolean medicalRequired;

	private BigDecimal premiumMonthly;
	private BigDecimal instalment;
	private Integer installments;
	private BigDecimal premiumAnnual;

	private List<LifeRiderDto> riders;

	private int headcount;
	private int averageAge;
	private int industryClass;
	private BigDecimal participation;
	private boolean includeSpouse;
	private boolean includeAdultChild;
	private Integer insuredCount;
	private BigDecimal programCost;

	private LifeTariffVersionDto tariffVersion;

	/**
	 * Rozkład wyborów pracowników: kod wariantu → liczba osób. Pusta mapa znaczy faza oferty,
	 * a nie „nikt nie wybrał" — to rozróżnienie niesie D-2 i front musi je widzieć.
	 */
	private Map<String, Integer> enrolment;

	/** Reżim analizy potrzeb — z reguł. Front nie zna daty progu. */
	private RegimeDto apkRegime;

	/** Ankieta APK: pytania razem z odpowiedziami, w kolejności z reguł. */
	private List<QuestionDto> apkQuestions;

	/** Wynik analizy: potrzeby, wyłączenia, propozycja i uzasadnienie. */
	private RecommendationDto apkRecommendation;

	public RecommendationDto getApkRecommendation() { return apkRecommendation; }
	public void setApkRecommendation(RecommendationDto apkRecommendation) { this.apkRecommendation = apkRecommendation; }

	public RegimeDto getApkRegime() { return apkRegime; }
	public void setApkRegime(RegimeDto apkRegime) { this.apkRegime = apkRegime; }
	public List<QuestionDto> getApkQuestions() { return apkQuestions; }
	public void setApkQuestions(List<QuestionDto> apkQuestions) { this.apkQuestions = apkQuestions; }

	private List<LifeVariantDto> variants;
	private List<LifeBenefitDto> benefits;

	public String getLineCode() { return lineCode; }
	public void setLineCode(String lineCode) { this.lineCode = lineCode; }
	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
	public BigDecimal getMainSum() { return mainSum; }
	public void setMainSum(BigDecimal mainSum) { this.mainSum = mainSum; }
	public String getPaymentFrequency() { return paymentFrequency; }
	public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }
	public LocalDate getBirthDate() { return birthDate; }
	public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
	public Integer getEntryAge() { return entryAge; }
	public void setEntryAge(Integer entryAge) { this.entryAge = entryAge; }
	public boolean isSmoker() { return smoker; }
	public void setSmoker(boolean smoker) { this.smoker = smoker; }
	public int getHeightCm() { return heightCm; }
	public void setHeightCm(int heightCm) { this.heightCm = heightCm; }
	public int getWeightKg() { return weightKg; }
	public void setWeightKg(int weightKg) { this.weightKg = weightKg; }
	public BigDecimal getBmi() { return bmi; }
	public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
	public int getOccupationClass() { return occupationClass; }
	public void setOccupationClass(int occupationClass) { this.occupationClass = occupationClass; }
	public BigDecimal getMonthlyIncome() { return monthlyIncome; }
	public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
	public LocalDate getPriorCoverFrom() { return priorCoverFrom; }
	public void setPriorCoverFrom(LocalDate priorCoverFrom) { this.priorCoverFrom = priorCoverFrom; }
	public LocalDate getPriorCoverTo() { return priorCoverTo; }
	public void setPriorCoverTo(LocalDate priorCoverTo) { this.priorCoverTo = priorCoverTo; }
	public String getDecision() { return decision; }
	public void setDecision(String decision) { this.decision = decision; }
	public boolean isBinding() { return binding; }
	public void setBinding(boolean binding) { this.binding = binding; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public boolean isMedicalRequired() { return medicalRequired; }
	public void setMedicalRequired(boolean medicalRequired) { this.medicalRequired = medicalRequired; }
	public BigDecimal getPremiumMonthly() { return premiumMonthly; }
	public void setPremiumMonthly(BigDecimal premiumMonthly) { this.premiumMonthly = premiumMonthly; }
	public BigDecimal getInstalment() { return instalment; }
	public void setInstalment(BigDecimal instalment) { this.instalment = instalment; }
	public Integer getInstallments() { return installments; }
	public void setInstallments(Integer installments) { this.installments = installments; }
	public BigDecimal getPremiumAnnual() { return premiumAnnual; }
	public void setPremiumAnnual(BigDecimal premiumAnnual) { this.premiumAnnual = premiumAnnual; }
	public List<LifeRiderDto> getRiders() { return riders; }
	public void setRiders(List<LifeRiderDto> riders) { this.riders = riders; }
	public int getHeadcount() { return headcount; }
	public void setHeadcount(int headcount) { this.headcount = headcount; }
	public int getAverageAge() { return averageAge; }
	public void setAverageAge(int averageAge) { this.averageAge = averageAge; }
	public int getIndustryClass() { return industryClass; }
	public void setIndustryClass(int industryClass) { this.industryClass = industryClass; }
	public BigDecimal getParticipation() { return participation; }
	public void setParticipation(BigDecimal participation) { this.participation = participation; }
	public boolean isIncludeSpouse() { return includeSpouse; }
	public void setIncludeSpouse(boolean includeSpouse) { this.includeSpouse = includeSpouse; }
	public boolean isIncludeAdultChild() { return includeAdultChild; }
	public void setIncludeAdultChild(boolean includeAdultChild) { this.includeAdultChild = includeAdultChild; }
	public Integer getInsuredCount() { return insuredCount; }
	public void setInsuredCount(Integer insuredCount) { this.insuredCount = insuredCount; }
	public BigDecimal getProgramCost() { return programCost; }
	public void setProgramCost(BigDecimal programCost) { this.programCost = programCost; }
	public LifeTariffVersionDto getTariffVersion() { return tariffVersion; }
	public void setTariffVersion(LifeTariffVersionDto tariffVersion) { this.tariffVersion = tariffVersion; }
	public Map<String, Integer> getEnrolment() { return enrolment; }
	public void setEnrolment(Map<String, Integer> enrolment) { this.enrolment = enrolment; }
	public List<LifeVariantDto> getVariants() { return variants; }
	public void setVariants(List<LifeVariantDto> variants) { this.variants = variants; }
	public List<LifeBenefitDto> getBenefits() { return benefits; }
	public void setBenefits(List<LifeBenefitDto> benefits) { this.benefits = benefits; }
}
