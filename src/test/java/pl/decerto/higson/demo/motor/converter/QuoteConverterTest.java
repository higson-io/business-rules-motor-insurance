package pl.decerto.higson.demo.motor.converter;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.decerto.higson.demo.motor.domain.Discount;
import pl.decerto.higson.demo.motor.domain.Quote;
import pl.decerto.higson.demo.motor.api.dto.CoverageDto;
import pl.decerto.higson.demo.motor.api.dto.DiscountDto;
import pl.decerto.higson.demo.motor.api.dto.QuoteDto;
import pl.decerto.higson.demo.motor.domain.Coverage;
import pl.decerto.higson.demo.motor.domain.Option;

public class QuoteConverterTest {

	private QuoteConverter quoteConverter;

	@Before
	public void setUp() throws Exception {
		quoteConverter = new QuoteConverter();
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenQuoteIsNull() throws Exception {
		quoteConverter.convert(null);
	}

	@Test
	public void shouldConvertQuote() throws Exception {
		//given
		Quote quote = new Quote();
		Option option = new Option("code", 1);
		Coverage coverage = new Coverage("C1", "coverage");
		coverage.setPremium(new BigDecimal(100));
		option.getCoverages().add(coverage);
		Discount discount = new Discount("D1", "discount", new BigDecimal(10), 1);
		option.getDiscounts().add(discount);
		quote.getOptions().add(option);
		//when
		QuoteDto result = quoteConverter.convert(quote);

		//then
		Assert.assertNotNull(result);
		Assert.assertTrue(CollectionUtils.isNotEmpty(result.getOptions()));
		Assert.assertNotNull(quote.getOptions().get(0).getCode(), result.getOptions().get(0).getCode());
		Assert.assertEquals(quote.getOptions().get(0).getOrder(), result.getOptions().get(0).getOrder());

		List<CoverageDto> coverages = result.getOptions().get(0).getCoverages();
		Assert.assertTrue(CollectionUtils.isNotEmpty(coverages));
		Assert.assertEquals(coverages.get(0).getCode(), coverage.getCode());
		Assert.assertEquals(coverages.get(0).getName(), coverage.getName());
		Assert.assertEquals(coverages.get(0).getPremium(), coverage.getPremium());

		List<DiscountDto> discounts = result.getOptions().get(0).getDiscounts();
		Assert.assertTrue(CollectionUtils.isNotEmpty(discounts));
		Assert.assertEquals(discounts.get(0).getCode(), discount.getCode());
		Assert.assertEquals(discounts.get(0).getName(), discount.getName());
		Assert.assertEquals(discounts.get(0).getValue(), discount.getValue());

		BigDecimal optionPremium = result.getOptions().get(0).getPremium();
		Assert.assertNotNull(optionPremium);
		Assert.assertEquals(optionPremium, new BigDecimal(90));
	}
}