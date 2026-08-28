package pl.decerto.higson.demo.property.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Oferta, która po akceptacji staje się polisą.
 *
 * ⛔ JEDEN obiekt na oba stany, a nie dwa. To jest ten sam dokument: oferta ma numer, datę
 * wystawienia i termin ważności, a akceptacja dokłada jej numer polisy i okres ochrony. Rozbicie
 * na `Offer` i `Policy` kusiłoby do skopiowania wszystkiego z jednego do drugiego, a wtedy
 * pierwsza zmiana warunków rozjeżdża dwie kopie.
 *
 * `policyNumber == null` znaczy „jeszcze oferta". Innego znacznika stanu nie ma i nie trzeba.
 */
public class Offer {

	private String number;
	private LocalDate issuedOn;
	private LocalDate validUntil;
	private LocalDate paymentDueOn;

	/** Treść oświadczenia klienta — z reguł, bo zmienia się razem z reżimem APK. */
	private String statement;

	/** Rek. 8.7 — czy do oferty dołącza się dokument analizy potrzeb. Też z reguł. */
	private boolean needsAnalysisAttached;

	private String policyNumber;
	private LocalDate coverFrom;
	private LocalDate coverTo;

	/**
	 * Wariant i składka ZAMROŻONE w chwili wystawienia.
	 *
	 * ⛔ Bez tego wydana polisa dalej pokazywałaby liczby z żywej wyceny: wystarczyło wrócić do
	 * ankiety, zmienić metraż i polisa cicho zmieniała składkę. Dokument wydany jest dokumentem —
	 * żeby zmienić ochronę, trzeba go cofnąć i wystawić na nowo, i tak właśnie działa przycisk
	 * „Wróć do oferty".
	 */
	private String variantCode;
	private BigDecimal premium;

	/**
	 * Rek. 9.3 — czy klient wziął to, co rekomendowaliśmy.
	 *
	 * ⛔ Odstąpienie od rekomendacji ma być ODNOTOWANE, a nie przemilczane. Trzy stany
	 * (`MATCHES` / `RICHER` / `POORER`), bo wariant szerszy od rekomendowanego niesie inne ryzyko
	 * niż węższy: przy węższym część zgłoszonych potrzeb zostaje bez pokrycia.
	 */
	private String choiceKind;
	private String choiceLabel;
	private String choiceText;
	private boolean choiceRequiresAcknowledgement;

	/**
	 * Potrzeby, których NIE pokrywa wariant WYBRANY przez klienta.
	 *
	 * ⛔ Liczone względem wyboru, a nie względem rekomendacji. `Recommendation.missingCovers` mówi,
	 * czego brakuje w wariancie PROPONOWANYM — a gdy klient zejdzie niżej, bez pokrycia zostaje
	 * więcej. Notatka rek. 9.3 twierdzi, że klient „został poinformowany, które potrzeby pozostają
	 * bez pokrycia"; bez tej listy byłoby to zdanie nieprawdziwe.
	 */
	private List<String> unmetNeeds = new ArrayList<>();

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public LocalDate getIssuedOn() {
		return issuedOn;
	}

	public void setIssuedOn(LocalDate issuedOn) {
		this.issuedOn = issuedOn;
	}

	public LocalDate getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(LocalDate validUntil) {
		this.validUntil = validUntil;
	}

	public LocalDate getPaymentDueOn() {
		return paymentDueOn;
	}

	public void setPaymentDueOn(LocalDate paymentDueOn) {
		this.paymentDueOn = paymentDueOn;
	}

	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

	public boolean isNeedsAnalysisAttached() {
		return needsAnalysisAttached;
	}

	public void setNeedsAnalysisAttached(boolean needsAnalysisAttached) {
		this.needsAnalysisAttached = needsAnalysisAttached;
	}

	public String getPolicyNumber() {
		return policyNumber;
	}

	public void setPolicyNumber(String policyNumber) {
		this.policyNumber = policyNumber;
	}

	public LocalDate getCoverFrom() {
		return coverFrom;
	}

	public void setCoverFrom(LocalDate coverFrom) {
		this.coverFrom = coverFrom;
	}

	public LocalDate getCoverTo() {
		return coverTo;
	}

	public void setCoverTo(LocalDate coverTo) {
		this.coverTo = coverTo;
	}

	public String getVariantCode() {
		return variantCode;
	}

	public void setVariantCode(String variantCode) {
		this.variantCode = variantCode;
	}

	public BigDecimal getPremium() {
		return premium;
	}

	public void setPremium(BigDecimal premium) {
		this.premium = premium;
	}

	public String getChoiceKind() { return choiceKind; }
	public void setChoiceKind(String choiceKind) { this.choiceKind = choiceKind; }
	public String getChoiceLabel() { return choiceLabel; }
	public void setChoiceLabel(String choiceLabel) { this.choiceLabel = choiceLabel; }
	public String getChoiceText() { return choiceText; }
	public void setChoiceText(String choiceText) { this.choiceText = choiceText; }
	public boolean isChoiceRequiresAcknowledgement() { return choiceRequiresAcknowledgement; }
	public void setChoiceRequiresAcknowledgement(boolean value) {
		this.choiceRequiresAcknowledgement = value;
	}

	public List<String> getUnmetNeeds() {
		return unmetNeeds;
	}

	public void setUnmetNeeds(List<String> unmetNeeds) {
		this.unmetNeeds = unmetNeeds;
	}

	public boolean isIssued() {
		return policyNumber != null;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("number", number)
			.append("policyNumber", policyNumber)
			.toString();
	}
}
