package pl.decerto.higson.demo.life.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Agregat wniosku życiowego, trzymany w sesji między żądaniami.
 *
 * Pola wyniku (decision, binding, premium*, variants) są wypełniane wyłącznie przez Higsona —
 * Java niczego z nich nie wylicza.
 */
public class LifeQuote {

	public static final String LINE_INDIVIDUAL = "INDIVIDUAL";
	public static final String LINE_GROUP = "GROUP";

	private String lineCode = LINE_INDIVIDUAL;

	private Insured insured = new Insured();
	private Policy policy = new Policy();
	private List<Rider> riders = new ArrayList<>();

	private GroupScheme group = new GroupScheme();
	private List<Variant> variants = new ArrayList<>();
	private List<Benefit> benefits = new ArrayList<>();

	/** ACCEPT / REFER / DECLINE — patrz decyzja D-1. */
	private String decision;

	/**
	 * Czy pokazana cena jest wiążąca. Osobne pole, a nie brak premium: inaczej front nie odróżni
	 * "brak ceny bo odmowa" od "brak ceny bo błąd", ani ceny wiążącej od wstępnej.
	 */
	private boolean binding;

	private String message;
	private boolean medicalRequired;

	private BigDecimal premiumMonthly;
	private BigDecimal instalment;
	private Integer installments;
	private BigDecimal premiumAnnual;

	private BigDecimal programCost;
	private Integer insuredCount;

	private TariffVersion tariffVersion = new TariffVersion();

	public TariffVersion getTariffVersion() {
		return tariffVersion;
	}

	public void setTariffVersion(TariffVersion tariffVersion) {
		this.tariffVersion = tariffVersion;
	}

	public List<Rider> getSelectedRiders() {
		List<Rider> selected = new ArrayList<>();
		for (Rider rider : riders) {
			if (rider.isSelected()) {
				selected.add(rider);
			}
		}
		return selected;
	}

	public Rider getRider(String code) {
		for (Rider rider : riders) {
			if (rider.getCode().equals(code)) {
				return rider;
			}
		}
		return null;
	}

	public String getLineCode() {
		return lineCode;
	}

	public void setLineCode(String lineCode) {
		this.lineCode = lineCode;
	}

	public Insured getInsured() {
		return insured;
	}

	public void setInsured(Insured insured) {
		this.insured = insured;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public List<Rider> getRiders() {
		return riders;
	}

	public void setRiders(List<Rider> riders) {
		this.riders = riders;
	}

	public GroupScheme getGroup() {
		return group;
	}

	public void setGroup(GroupScheme group) {
		this.group = group;
	}

	public List<Variant> getVariants() {
		return variants;
	}

	public void setVariants(List<Variant> variants) {
		this.variants = variants;
	}

	public List<Benefit> getBenefits() {
		return benefits;
	}

	public void setBenefits(List<Benefit> benefits) {
		this.benefits = benefits;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public boolean isBinding() {
		return binding;
	}

	public void setBinding(boolean binding) {
		this.binding = binding;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isMedicalRequired() {
		return medicalRequired;
	}

	public void setMedicalRequired(boolean medicalRequired) {
		this.medicalRequired = medicalRequired;
	}

	public BigDecimal getPremiumMonthly() {
		return premiumMonthly;
	}

	public void setPremiumMonthly(BigDecimal premiumMonthly) {
		this.premiumMonthly = premiumMonthly;
	}

	public BigDecimal getInstalment() {
		return instalment;
	}

	public void setInstalment(BigDecimal instalment) {
		this.instalment = instalment;
	}

	public Integer getInstallments() {
		return installments;
	}

	public void setInstallments(Integer installments) {
		this.installments = installments;
	}

	public BigDecimal getPremiumAnnual() {
		return premiumAnnual;
	}

	public void setPremiumAnnual(BigDecimal premiumAnnual) {
		this.premiumAnnual = premiumAnnual;
	}

	public BigDecimal getProgramCost() {
		return programCost;
	}

	public void setProgramCost(BigDecimal programCost) {
		this.programCost = programCost;
	}

	public Integer getInsuredCount() {
		return insuredCount;
	}

	public void setInsuredCount(Integer insuredCount) {
		this.insuredCount = insuredCount;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("lineCode", lineCode)
			.append("decision", decision)
			.append("premiumMonthly", premiumMonthly)
			.toString();
	}
}
