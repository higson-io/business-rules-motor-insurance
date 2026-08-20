package pl.decerto.higson.demo.life.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import pl.decerto.higson.demo.life.api.dto.LifeBenefitDto;
import pl.decerto.higson.demo.life.api.dto.LifeQuoteDto;
import pl.decerto.higson.demo.life.api.dto.LifeRiderDto;
import pl.decerto.higson.demo.life.api.dto.LifeVariantDto;
import pl.decerto.higson.demo.life.domain.Benefit;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;
import pl.decerto.higson.demo.life.domain.Variant;

@Component
public class LifeQuoteConverter implements Converter<LifeQuote, LifeQuoteDto> {

	@Override
	public LifeQuoteDto convert(LifeQuote quote) {
		LifeQuoteDto dto = new LifeQuoteDto();

		dto.setLineCode(quote.getLineCode());

		dto.setStartDate(quote.getPolicy().getStartDate());
		dto.setMainSum(quote.getPolicy().getMainSum());
		dto.setPaymentFrequency(quote.getPolicy().getPaymentFrequency());

		dto.setBirthDate(quote.getInsured().getBirthDate());
		dto.setEntryAge(quote.getInsured().getEntryAge(quote.getPolicy().getStartDate()));
		dto.setSmoker(quote.getInsured().isSmoker());
		dto.setHeightCm(quote.getInsured().getHeightCm());
		dto.setWeightKg(quote.getInsured().getWeightKg());
		dto.setBmi(quote.getInsured().getBmi());
		dto.setOccupationClass(quote.getInsured().getOccupationClass());
		dto.setMonthlyIncome(quote.getInsured().getMonthlyIncome());
		dto.setPriorCoverFrom(quote.getInsured().getPriorCoverFrom());
		dto.setPriorCoverTo(quote.getInsured().getPriorCoverTo());

		dto.setDecision(quote.getDecision());
		dto.setBinding(quote.isBinding());
		dto.setMessage(quote.getMessage());
		dto.setMedicalRequired(quote.isMedicalRequired());

		dto.setPremiumMonthly(quote.getPremiumMonthly());
		dto.setInstalment(quote.getInstalment());
		dto.setInstallments(quote.getInstallments());
		dto.setPremiumAnnual(quote.getPremiumAnnual());

		dto.setRiders(convertRiders(quote.getRiders()));

		dto.setHeadcount(quote.getGroup().getHeadcount());
		dto.setAverageAge(quote.getGroup().getAverageAge());
		dto.setIndustryClass(quote.getGroup().getIndustryClass());
		dto.setParticipation(quote.getGroup().getParticipation());
		dto.setIncludeSpouse(quote.getGroup().isIncludeSpouse());
		dto.setIncludeAdultChild(quote.getGroup().isIncludeAdultChild());
		dto.setInsuredCount(quote.getInsuredCount());
		dto.setProgramCost(quote.getProgramCost());

		dto.setVariants(convertVariants(quote.getVariants()));
		dto.setBenefits(convertBenefits(quote.getBenefits()));

		return dto;
	}

	private List<LifeRiderDto> convertRiders(List<Rider> riders) {
		List<LifeRiderDto> result = new ArrayList<>();
		for (Rider rider : riders) {
			LifeRiderDto dto = new LifeRiderDto();
			dto.setCode(rider.getCode());
			dto.setName(rider.getName());
			dto.setDescription(rider.getDescription());
			dto.setPosition(rider.getPosition());
			dto.setBenefitKind(rider.getBenefitKind());
			dto.setSums(rider.getSums());
			dto.setDefaultSum(rider.getDefaultSum());
			dto.setMaxSum(rider.getMaxSum());
			dto.setWaitingPeriodDays(rider.getWaitingPeriodDays());
			dto.setSelected(rider.isSelected());
			dto.setSum(rider.getSum());
			dto.setPremium(rider.getPremium());
			result.add(dto);
		}
		return result;
	}

	private List<LifeVariantDto> convertVariants(List<Variant> variants) {
		List<LifeVariantDto> result = new ArrayList<>();
		for (Variant variant : variants) {
			LifeVariantDto dto = new LifeVariantDto();
			dto.setCode(variant.getCode());
			dto.setName(variant.getName());
			dto.setPosition(variant.getPosition());
			dto.setAvailable(variant.isAvailable());
			dto.setPerEmployee(variant.getPerEmployee());
			dto.setMonthlyTotalIfAll(variant.getMonthlyTotalIfAll());
			result.add(dto);
		}
		return result;
	}

	private List<LifeBenefitDto> convertBenefits(List<Benefit> benefits) {
		List<LifeBenefitDto> result = new ArrayList<>();
		for (Benefit benefit : benefits) {
			LifeBenefitDto dto = new LifeBenefitDto();
			dto.setCode(benefit.getCode());
			dto.setName(benefit.getName());
			dto.setPosition(benefit.getPosition());
			dto.setBenefitKind(benefit.getBenefitKind());
			dto.setSumByVariant(benefit.getSumByVariant());
			result.add(dto);
		}
		return result;
	}
}
