package pl.decerto.hyperon.demo.motor.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.smartparam.engine.types.date.DateType;
import org.smartparam.engine.types.datetime.DatetimeType;
import org.smartparam.engine.types.integer.IntegerType;
import org.smartparam.engine.types.number.NumberType;
import org.smartparam.engine.types.string.StringType;

import pl.decerto.hyperon.demo.motor.domain.Address;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.demo.motor.domain.Option;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.ext.adapter.CollectionAdapter;
import pl.decerto.hyperon.runtime.core.AdhocContext;
import pl.decerto.hyperon.runtime.core.HyperonContext;
import pl.decerto.hyperon.runtime.core.HyperonSubContext;
import pl.decerto.hyperon.runtime.type.BooleanType;

public class MotorContextTest {

	@Before
	public void before() {
		HyperonContext.initialize(new StringType(), new NumberType(), new IntegerType(), new BooleanType(), new DateType(), new DatetimeType());
	}

	@Test
	public void shouldLoadPropertiesDynamically() {

		// given

		Coverage cov = new Coverage("BI", "BI");
		Driver driver = new Driver()
			.setFirstName("John")
			.setLastName("Potter")
			.setGender("M")
			.setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()));

		Quote quote = new Quote("FULL", driver);

		Option option = new Option("OPT1", 1);
		option.addCoverage(cov);

		quote.addOption(option);

		// when

		MotorContext ctx = new MotorContext(cov);

		// then

		assertSame(ctx.getCoverage(), cov);
		assertSame(ctx.getOption(), option);
		assertSame(ctx.getQuote(), quote);
	}

	@Test
	public void shouldResolvePaths() {

		Address adr = new Address("Lake Jackson", "Allwood St", "77566");
		Driver driver = new Driver()
			.setFirstName("John")
			.setLastName("Potter")
			.setGender("M")
			.setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()))
			.setAddress(adr);

		Coverage cov = new Coverage("BI", "BI");
		cov.setPremium(new BigDecimal(310));

		Quote quote = new Quote("FULL", driver);

		Option option1 = new Option("OPT1", 1);
		Option option2 = new Option("OPT2", 2);

		option1.addCoverage(cov);

		quote.addOption(option1);
		quote.addOption(option2);

		HyperonContext ctx = new MotorContext(cov);

		// paths starting from "quote."

		assertEquals(ctx.getString("quote.driver.address.zipcode"), "77566");
		assertEquals(ctx.getString("quote.driver.firstname"), "John");
		assertEquals(ctx.getString("quote.driver.lastname"), "Potter");
		assertEquals(ctx.getString("quote.driver.gender"), "M");
		assertEquals(ctx.getInteger("quote.driver.age").intValue(), 40);

		// paths starting from "coverage.option."

		assertEquals(ctx.getDecimal("coverage.premium"), new BigDecimal("310"));

		assertEquals(ctx.getString("coverage.option.quote.driver.address.zipcode"), "77566");
		assertEquals(ctx.getString("coverage.option.quote.driver.firstname"), "John");
		assertEquals(ctx.getString("coverage.option.quote.driver.gender"), "M");
		assertEquals(ctx.getInteger("coverage.option.quote.driver.age").intValue(), 40);

		// paths starting from "coverage.quote."

		assertEquals(ctx.getString("coverage.quote.driver.address.zipcode"), "77566");
		assertEquals(ctx.getString("coverage.quote.driver.firstname"), "John");
		assertEquals(ctx.getString("coverage.quote.driver.gender"), "M");
		assertEquals(ctx.getInteger("coverage.quote.driver.age").intValue(), 40);

		// paths starting from "option."

		assertEquals(ctx.getString("option.quote.driver.address.zipcode"), "77566");
		assertEquals(ctx.getString("option.quote.driver.firstname"), "John");
		assertEquals(ctx.getString("option.quote.driver.gender"), "M");
		assertEquals(ctx.getInteger("option.quote.driver.age").intValue(), 40);

		// collections taken from context

		assertTrue(ctx.get("quote.options") instanceof Iterable);
		assertTrue(ctx.get("quote.options") instanceof CollectionAdapter);

		@SuppressWarnings("unchecked")
		CollectionAdapter<Option> options = (CollectionAdapter) ctx.get("quote.options");

		assertEquals(options.size(), 2);
		assertEquals(options.isEmpty(), false);
		assertEquals(options.isNotEmpty(), true);

		for (HyperonContext option : options) {
			String code = option.getString("code");
			assertTrue(code.equals("OPT1") || code.equals("OPT2"));
		}

	}

	@Test
	public void shouldGetCollection() {

		Address adr = new Address("Lake Jackson", "Allwood St", "77566");
		Driver driver = new Driver()
			.setFirstName("John")
			.setLastName("Potter")
			.setGender("M")
			.setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()))
			.setAddress(adr);

		Coverage cov1 = new Coverage("BI", "BI");
		cov1.setPremium(new BigDecimal(110));
		Coverage cov2 = new Coverage("BI", "BI");
		cov2.setPremium(new BigDecimal(220));

		Quote quote = new Quote("FULL", driver);

		// OPT1 - 2 coverages
		Option option1 = new Option("OPT1", 1);
		option1.addCoverage(cov1);
		option1.addCoverage(cov2);

		// OPT2 - 0 coverages
		Option option2 = new Option("OPT2", 2);

		quote.addOption(option1);
		quote.addOption(option2);

		HyperonContext ctx = new MotorContext(cov1);

		// when

		@SuppressWarnings("unchecked")
		CollectionAdapter<Option> collection = (CollectionAdapter) ctx.get("quote.options");

		// then

		// loop
		for (HyperonSubContext option : collection) {

			String optionCode = option.getString("code");

			// total premium for this option
			int totalPremium = 0;

			// inner loop
			@SuppressWarnings("unchecked")
			CollectionAdapter<Coverage> coverages = (CollectionAdapter) option.get("coverages");

			for (HyperonContext c : coverages) {
				totalPremium += c.getInteger("premium");
			}

			// verify option
			switch (optionCode) {
				case "OPT1":
					assertEquals(totalPremium, 330);
					break;

				case "OPT2":
					assertEquals(totalPremium, 0);
					break;

				default:
					fail();
					break;
			}
		}

		// some collection methods
		assertEquals(collection.size(), 2);
		assertEquals(collection.isEmpty(), false);
		assertEquals(collection.isNotEmpty(), true);
	}

	@Test
	public void shouldUseAdhocSubContext() {

		Address adr = new Address("Lake Jackson", "Allwood St", "77566");
		Driver driver = new Driver()
			.setFirstName("John")
			.setLastName("Potter")
			.setGender("M")
			.setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()))
			.setAddress(adr);

		Coverage cov1 = new Coverage("BI", "BI");
		cov1.setPremium(new BigDecimal(110));
		Coverage cov2 = new Coverage("PD", "PD");
		cov2.setPremium(new BigDecimal(220));

		Quote quote = new Quote("FULL", driver);

		// OPT1 - 2 coverages
		Option option1 = new Option("OPT1", 1);
		option1.addCoverage(cov1);
		option1.addCoverage(cov2);

		// OPT2 - 0 coverages
		Option option2 = new Option("OPT2", 2);

		quote.addOption(option1);
		quote.addOption(option2);

		HyperonContext ctx = new MotorContext(cov1);

		// ctx knows "option" because it is anchored at cov1 - which belongs to OPT1
		@SuppressWarnings("unchecked")
		CollectionAdapter<Option> collection = (CollectionAdapter) ctx.get("option.coverages");


		int i = 0;
		for (HyperonSubContext sub : collection) {

			// standard use
			assertEquals(sub.getInteger("premium").intValue(), i == 0 ? 110 : 220);

			// adhoc context with 'coverage' as current iteration subcontext
			HyperonContext adhoc = new AdhocContext(ctx, "coverage", sub);

			// then
			assertEquals(adhoc.getInteger("coverage.premium"), sub.getInteger("premium"));
			assertEquals(adhoc.getString("coverage.code"), i == 0 ? "BI" : "PD");
			assertEquals(adhoc.getString("coverage.quote.planCode"), "FULL");

			i++;
		}
	}

	@Test
	public void shouldUseSubContext() {

		HyperonContext ctx = new HyperonContext();
		assertEquals(ctx.get("someProperty"), null);

		HyperonContext sub = new HyperonContext("code", "BI");
		ctx = new HyperonContext().set("coverage", sub);

		assertEquals(ctx.get("coverage"), sub);
		assertEquals(ctx.get("coverage.code"), "BI");
	}
}
