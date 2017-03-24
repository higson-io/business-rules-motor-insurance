package pl.decerto.hyperon.demo.motor.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pl.decerto.hyperon.demo.motor.api.dto.DiscountDto;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.demo.motor.domain.Discount;
import pl.decerto.hyperon.demo.motor.domain.Option;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.demo.motor.api.dto.CoverageDto;
import pl.decerto.hyperon.demo.motor.api.dto.OptionDto;
import pl.decerto.hyperon.demo.motor.api.dto.QuoteDto;

@Component
public class QuoteConverter implements Converter<Quote, QuoteDto> {

	@Override
	public QuoteDto convert(Quote quote) {
		Objects.requireNonNull(quote, "Quote can not be null");
		QuoteDto dto = new QuoteDto();
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
		BigDecimal optionPremium = getOptionPremium(option);
		dto.setPremium(optionPremium);
		dto.setPremiumBeforeDiscounts(optionPremium);
		updateOptionWithDiscounts(dto);
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

	private BigDecimal getOptionPremium(Option option) {
		return option.getCoverages()
				.stream()
				.map(Coverage::getPremium)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}

	private void updateOptionWithDiscounts(OptionDto option) {
		List<DiscountDto> discounts = option.getDiscounts();
		for (DiscountDto discount : discounts) {
			BigDecimal oldOptionPremium = option.getPremium();
			option.setPremium(oldOptionPremium.subtract(discount.getValue()));
		}
	}
}
