package pl.decerto.higson.demo.property.converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import pl.decerto.higson.demo.property.api.dto.PropertyCoverDto;
import pl.decerto.higson.demo.property.api.dto.PropertyQuestionDto;
import pl.decerto.higson.demo.apk.api.RecommendationDto;
import pl.decerto.higson.demo.property.api.dto.PropertyOfferDto;
import pl.decerto.higson.demo.property.api.dto.PropertyPolicyholderDto;
import pl.decerto.higson.demo.property.api.dto.PropertyQuoteDto;
import pl.decerto.higson.demo.property.api.dto.PropertyVariantDto;
import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.property.domain.Cover;
import pl.decerto.higson.demo.property.domain.Offer;
import pl.decerto.higson.demo.property.domain.Policyholder;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.property.domain.Variant;

@Component
public class PropertyQuoteConverter implements Converter<PropertyQuote, PropertyQuoteDto> {

	@Override
	public PropertyQuoteDto convert(PropertyQuote quote) {
		PropertyQuoteDto dto = new PropertyQuoteDto();

		dto.setStartDate(quote.getPolicy().getStartDate());
		dto.setDeductible(quote.getPolicy().getDeductible());
		dto.setFloodDeductible(quote.getFloodDeductible());

		dto.setQuestions(convertQuestions(quote));

		dto.setFloodZone(quote.getZone().getFlood());
		dto.setTheftZone(quote.getZone().getTheft());
		dto.setRiskFactor(quote.getRiskFactor());

		dto.setSumBuilding(quote.getSums().getBuilding());
		dto.setSumFixtures(quote.getSums().getFixtures());
		dto.setSumContents(quote.getSums().getContents());
		dto.setSuggestedSumBuilding(quote.getSums().getSuggestedBuilding());

		dto.setCovers(convertCovers(quote.getCovers()));
		dto.setVariants(convertVariants(quote.getVariants()));
		// Potrzeby jadą razem z propozycją, bo razem są jedną analizą — patrz RecommendationDto.
		dto.setRecommendation(RecommendationDto.of(quote.getRecommendation(), quote.getNeeds()));
		dto.setOffer(convertOffer(quote.getOffer()));
		dto.setPolicyholder(convertPolicyholder(quote.getPolicyholder()));
		dto.setSelectedVariant(quote.getSelectedVariant());
		dto.setVariantChosen(quote.isVariantChosen());

		return dto;
	}

	private PropertyOfferDto convertOffer(Offer offer) {
		PropertyOfferDto dto = new PropertyOfferDto();
		dto.setNumber(offer.getNumber());
		dto.setIssuedOn(offer.getIssuedOn());
		dto.setValidUntil(offer.getValidUntil());
		dto.setPaymentDueOn(offer.getPaymentDueOn());
		dto.setStatement(offer.getStatement());
		dto.setNeedsAnalysisAttached(offer.isNeedsAnalysisAttached());
		dto.setPolicyNumber(offer.getPolicyNumber());
		dto.setCoverFrom(offer.getCoverFrom());
		dto.setCoverTo(offer.getCoverTo());
		dto.setChoiceKind(offer.getChoiceKind());
		dto.setChoiceLabel(offer.getChoiceLabel());
		dto.setChoiceText(offer.getChoiceText());
		dto.setChoiceRequiresAcknowledgement(offer.isChoiceRequiresAcknowledgement());
		dto.setUnmetNeeds(offer.getUnmetNeeds());
		dto.setVariantCode(offer.getVariantCode());
		dto.setPremium(offer.getPremium());
		return dto;
	}

	private PropertyPolicyholderDto convertPolicyholder(Policyholder source) {
		PropertyPolicyholderDto dto = new PropertyPolicyholderDto();
		dto.setFirstName(source.getFirstName());
		dto.setLastName(source.getLastName());
		dto.setPesel(source.getPesel());
		dto.setBirthDate(source.getBirthDate());
		dto.setIdDocument(source.getIdDocument());
		dto.setStreet(source.getStreet());
		dto.setFlat(source.getFlat());
		dto.setPostcode(source.getPostcode());
		dto.setCity(source.getCity());
		dto.setEmail(source.getEmail());
		dto.setPhone(source.getPhone());
		dto.setSource(source.getSource());
		return dto;
	}


	/**
	 * Pytania na drut — i JEDYNE miejsce, w którym płaska lista staje się drzewem.
	 *
	 * ⛔ Pole pytania złożonego NIE trafia na najwyższy poziom, tylko pod swojego rodzica. Gdyby
	 * trafiło, ekran policzyłby je jako osobne pytanie („1 z 8" zamiast „1 z 7") i pokazał je
	 * dwa razy — raz w środku rodzica, raz obok niego.
	 *
	 * ⚠️ Pole, którego rodzica nie ma na liście (literówka w kolumnie `partOf`, rodzic odcięty
	 * regułą widoczności), zostaje pytaniem samodzielnym. Ciche zniknięcie byłoby gorsze:
	 * konfigurator zobaczyłby brakujące pytanie i nie miałby jak zgadnąć, czym się różni od
	 * pytania niewidocznego.
	 */
	private List<PropertyQuestionDto> convertQuestions(PropertyQuote quote) {
		List<PropertyQuestionDto> result = new ArrayList<>();
		Map<String, PropertyQuestionDto> byCode = new LinkedHashMap<>();
		List<Question> fields = new ArrayList<>();

		for (Question question : quote.getQuestions()) {
			PropertyQuestionDto dto = convertQuestion(quote, question);
			byCode.put(question.getCode(), dto);
			if (question.isField()) {
				fields.add(question);
			} else {
				result.add(dto);
			}
		}

		for (Question field : fields) {
			PropertyQuestionDto parent = byCode.get(field.getPartOf());
			PropertyQuestionDto dto = byCode.get(field.getCode());
			if (parent == null) {
				result.add(dto);
			} else {
				parent.getFields().add(dto);
			}
		}
		return result;
	}

	/** Jedno pytanie — bez wiedzy o tym, czy jest polem czy stoi samo. */
	private PropertyQuestionDto convertQuestion(PropertyQuote quote, Question question) {
		PropertyQuestionDto dto = new PropertyQuestionDto();
		dto.setCode(question.getCode());
		dto.setText(question.getText());
		dto.setKind(question.getKind());
		dto.setSection(question.getSection());
		dto.setPosition(question.getPosition());
		dto.setVisible(question.isVisible());
		dto.setRequired(question.isRequired());
		dto.setOptions(question.getOptions());
		dto.setOptionLabels(question.getOptionLabels());
		dto.setPrefillSource(question.getPrefillSource());
		dto.setRefusalMessage(question.getRefusalMessage());

		Answer answer = quote.getAnswers().get(question.getCode());
		if (answer != null) {
			dto.setValue(answer.getValue());
			dto.setValues(answer.getValues());
			dto.setRefused(answer.isRefused());
		} else {
			dto.setValues(new ArrayList<>());
		}
		return dto;
	}

	private List<PropertyCoverDto> convertCovers(List<Cover> covers) {
		List<PropertyCoverDto> result = new ArrayList<>();
		for (Cover cover : covers) {
			PropertyCoverDto dto = new PropertyCoverDto();
			dto.setCode(cover.getCode());
			dto.setName(cover.getName());
			dto.setDescription(cover.getDescription());
			dto.setPosition(cover.getPosition());
			dto.setCapLabel(cover.getCapLabel());
			dto.setEffectiveLimit(cover.getEffectiveLimit());
			dto.setSum(cover.getSum());
			result.add(dto);
		}
		return result;
	}

	private List<PropertyVariantDto> convertVariants(List<Variant> variants) {
		List<PropertyVariantDto> result = new ArrayList<>();
		for (Variant variant : variants) {
			PropertyVariantDto dto = new PropertyVariantDto();
			dto.setCode(variant.getCode());
			dto.setName(variant.getName());
			dto.setDescription(variant.getDescription());
			dto.setPosition(variant.getPosition());
			dto.setAvailable(variant.isAvailable());
			dto.setAnnualPremium(variant.getAnnualPremium());
			dto.setInstalment(variant.getInstalment());
			dto.setBreakdown(variant.getBreakdown());
			dto.setCovers(new ArrayList<>(variant.getCovers()));
			result.add(dto);
		}
		return result;
	}



}
