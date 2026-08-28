package pl.decerto.higson.demo.property.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Oferta na drucie. `policyNumber` puste znaczy „jeszcze oferta" — patrz `Offer`, jeden obiekt
 * na oba stany.
 */
public class PropertyOfferDto {

	private String number;
	private LocalDate issuedOn;
	private LocalDate validUntil;
	private LocalDate paymentDueOn;
	private String statement;
	private boolean needsAnalysisAttached;

	private String policyNumber;
	private LocalDate coverFrom;
	private LocalDate coverTo;

	/** Zamrożone przy wystawieniu — patrz `Offer`. Puste, dopóki to oferta. */
	private String variantCode;
	private BigDecimal premium;

	/** Rek. 9.3 — notatka o zgodności wyboru z rekomendacją. */
	private String choiceKind;
	private String choiceLabel;
	private String choiceText;
	private boolean choiceRequiresAcknowledgement;
	private List<String> unmetNeeds = new ArrayList<>();

	public String getNumber() { return number; }
	public void setNumber(String number) { this.number = number; }
	public LocalDate getIssuedOn() { return issuedOn; }
	public void setIssuedOn(LocalDate issuedOn) { this.issuedOn = issuedOn; }
	public LocalDate getValidUntil() { return validUntil; }
	public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
	public LocalDate getPaymentDueOn() { return paymentDueOn; }
	public void setPaymentDueOn(LocalDate paymentDueOn) { this.paymentDueOn = paymentDueOn; }
	public String getStatement() { return statement; }
	public void setStatement(String statement) { this.statement = statement; }
	public boolean isNeedsAnalysisAttached() { return needsAnalysisAttached; }
	public void setNeedsAnalysisAttached(boolean value) { this.needsAnalysisAttached = value; }
	public String getPolicyNumber() { return policyNumber; }
	public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
	public LocalDate getCoverFrom() { return coverFrom; }
	public void setCoverFrom(LocalDate coverFrom) { this.coverFrom = coverFrom; }
	public LocalDate getCoverTo() { return coverTo; }
	public void setCoverTo(LocalDate coverTo) { this.coverTo = coverTo; }
	public String getVariantCode() { return variantCode; }
	public void setVariantCode(String variantCode) { this.variantCode = variantCode; }
	public BigDecimal getPremium() { return premium; }
	public void setPremium(BigDecimal premium) { this.premium = premium; }
	public String getChoiceKind() { return choiceKind; }
	public void setChoiceKind(String choiceKind) { this.choiceKind = choiceKind; }
	public String getChoiceLabel() { return choiceLabel; }
	public void setChoiceLabel(String choiceLabel) { this.choiceLabel = choiceLabel; }
	public String getChoiceText() { return choiceText; }
	public void setChoiceText(String choiceText) { this.choiceText = choiceText; }
	public List<String> getUnmetNeeds() { return unmetNeeds; }
	public void setUnmetNeeds(List<String> unmetNeeds) { this.unmetNeeds = unmetNeeds; }
	public boolean isChoiceRequiresAcknowledgement() { return choiceRequiresAcknowledgement; }
	public void setChoiceRequiresAcknowledgement(boolean v) { this.choiceRequiresAcknowledgement = v; }
}
