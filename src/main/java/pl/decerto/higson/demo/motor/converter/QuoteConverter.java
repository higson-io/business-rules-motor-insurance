package pl.decerto.higson.demo.motor.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pl.decerto.higson.demo.apk.api.QuestionDto;
import pl.decerto.higson.demo.apk.api.RecommendationDto;
import pl.decerto.higson.demo.apk.api.RegimeDto;
import pl.decerto.higson.demo.motor.api.dto.CoverageDto;
import pl.decerto.higson.demo.motor.api.dto.DiscountDto;
import pl.decerto.higson.demo.motor.api.dto.OptionDto;
import pl.decerto.higson.demo.motor.domain.Discount;
import pl.decerto.higson.demo.motor.domain.Quote;
import pl.decerto.higson.demo.motor.domain.Coverage;
import pl.decerto.higson.demo.motor.domain.Option;
import pl.decerto.higson.demo.motor.domain.OptionPricing;
import pl.decerto.higson.demo.motor.api.dto.QuoteDto;

@Component
public class QuoteConverter implements Converter<Quote, QuoteDto> {

	@Override
	public QuoteDto convert(Quote quote) {
		Objects.requireNonNull(quote, "Quote can not be null");
		QuoteDto dto = new QuoteDto();
		dto.setPolicyStartDate(quote.getPolicyStartDate());
		dto.setApkRegime(RegimeDto.of(quote.getApkRegime()));
		dto.setApkQuestions(quote.getApkQuestions()
			.stream()
			.map(question -> QuestionDto.of(question, quote.getApkAnswers().get(question.getCode())))
			.collect(Collectors.toList()));
		dto.setApkRecommendation(RecommendationDto.of(
			quote.getApkRecommendation(), quote.getApkNeeds(), quote.getApkWarnings()));
		dto.setOptions(quote.getOptions()
			.stream()
			.map(this::toOptionDto)
			.collect(Collectors.toList()));
		return dto;
	}

	private OptionDto toOptionDto(Option option) {
		OptionDto dto = new OptionDto();
		dto.setCode(option.getCode());
		dto.setCoverages(convertCoverages(option.getCoverages()));
		dto.setDiscounts(convertDiscounts(option.getDiscounts()));
		dto.setOrder(option.getOrder());
		BigDecimal beforeDiscounts = OptionPricing.beforeDiscounts(option);
		dto.setPremium(OptionPricing.finalPremium(option));
		dto.setPremiumBeforeDiscounts(beforeDiscounts);
		return dto;
	}

	private List<CoverageDto> convertCoverages(List<Coverage> coverages) {
		return coverages
			.stream()
			.map(this::toCoverageDto)
			.collect(Collectors.toList());
	}

	private List<DiscountDto> convertDiscounts(List<Discount> discounts) {
		return discounts
				.stream()
				.map(this::toDiscountDto)
				.collect(Collectors.toList());
	}

	private CoverageDto toCoverageDto(Coverage coverage) {
		CoverageDto dto = new CoverageDto();
		dto.setCode(coverage.getCode());
		dto.setName(coverage.getName());
		dto.setDescription(coverage.getDescription());
		dto.setLimitLeft(coverage.getLimit1());
		dto.setLimitRight(coverage.getLimit2());
		dto.setPremium(coverage.getPremium());
		dto.setPosition(coverage.getPosition());
		return dto;
	}

	private DiscountDto toDiscountDto(Discount discount) {
		DiscountDto dto = new DiscountDto();
		dto.setName(discount.getName());
		dto.setCode(discount.getCode());
		dto.setValue(discount.getValue());
		dto.setPosition(discount.getPosition());
		return dto;
	}


}
