package pl.decerto.higson.demo.property.api.dto;

import pl.decerto.higson.demo.apk.api.RecommendationDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cały wniosek majątkowy w postaci, w jakiej widzi go ekran. */
public class PropertyQuoteDto {

	private LocalDate startDate;
	private BigDecimal deductible;

	private PropertyRegimeDto regime;
	private List<PropertySectionDto> sections;
	private List<PropertyQuestionDto> questions;

	private String floodZone;
	private String theftZone;
	private Integer buildingAge;
	private BigDecimal riskFactor;

	private BigDecimal sumBuilding;
	private BigDecimal sumFixtures;
	private BigDecimal sumContents;
	private BigDecimal suggestedSumBuilding;

	private List<PropertyCoverDto> covers;
	private List<PropertyVariantDto> variants;
	private RecommendationDto recommendation;
	private PropertyOfferDto offer;
	private PropertyPolicyholderDto policyholder;
	private String selectedVariant;

	/** Odpowiedź, po której nie ma czego dalej analizować — treść komunikatu podaje reguła. */
	private boolean blocked;
	private String blockMessage;

	public PropertyPolicyholderDto getPolicyholder() {
		return policyholder;
	}

	public void setPolicyholder(PropertyPolicyholderDto policyholder) {
		this.policyholder = policyholder;
	}

	public PropertyOfferDto getOffer() {
		return offer;
	}

	public void setOffer(PropertyOfferDto offer) {
		this.offer = offer;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}


	public BigDecimal getDeductible() {
		return deductible;
	}

	public void setDeductible(BigDecimal deductible) {
		this.deductible = deductible;
	}

	public PropertyRegimeDto getRegime() {
		return regime;
	}

	public void setRegime(PropertyRegimeDto regime) {
		this.regime = regime;
	}

	public List<PropertySectionDto> getSections() {
		return sections;
	}

	public void setSections(List<PropertySectionDto> sections) {
		this.sections = sections;
	}

	public List<PropertyQuestionDto> getQuestions() {
		return questions;
	}

	public void setQuestions(List<PropertyQuestionDto> questions) {
		this.questions = questions;
	}

	public String getFloodZone() {
		return floodZone;
	}

	public void setFloodZone(String floodZone) {
		this.floodZone = floodZone;
	}

	public String getTheftZone() {
		return theftZone;
	}

	public void setTheftZone(String theftZone) {
		this.theftZone = theftZone;
	}

	public Integer getBuildingAge() {
		return buildingAge;
	}

	public void setBuildingAge(Integer buildingAge) {
		this.buildingAge = buildingAge;
	}

	public BigDecimal getRiskFactor() {
		return riskFactor;
	}

	public void setRiskFactor(BigDecimal riskFactor) {
		this.riskFactor = riskFactor;
	}

	public BigDecimal getSumBuilding() {
		return sumBuilding;
	}

	public void setSumBuilding(BigDecimal sumBuilding) {
		this.sumBuilding = sumBuilding;
	}

	public BigDecimal getSumFixtures() {
		return sumFixtures;
	}

	public void setSumFixtures(BigDecimal sumFixtures) {
		this.sumFixtures = sumFixtures;
	}

	public BigDecimal getSumContents() {
		return sumContents;
	}

	public void setSumContents(BigDecimal sumContents) {
		this.sumContents = sumContents;
	}

	public BigDecimal getSuggestedSumBuilding() {
		return suggestedSumBuilding;
	}

	public void setSuggestedSumBuilding(BigDecimal suggestedSumBuilding) {
		this.suggestedSumBuilding = suggestedSumBuilding;
	}

	public List<PropertyCoverDto> getCovers() {
		return covers;
	}

	public void setCovers(List<PropertyCoverDto> covers) {
		this.covers = covers;
	}

	public List<PropertyVariantDto> getVariants() {
		return variants;
	}

	public void setVariants(List<PropertyVariantDto> variants) {
		this.variants = variants;
	}



	public RecommendationDto getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(RecommendationDto recommendation) {
		this.recommendation = recommendation;
	}

	public String getSelectedVariant() {
		return selectedVariant;
	}

	public void setSelectedVariant(String selectedVariant) {
		this.selectedVariant = selectedVariant;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public String getBlockMessage() {
		return blockMessage;
	}

	public void setBlockMessage(String blockMessage) {
		this.blockMessage = blockMessage;
	}
}
