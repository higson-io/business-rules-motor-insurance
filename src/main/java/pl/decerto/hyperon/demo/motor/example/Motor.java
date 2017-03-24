package pl.decerto.hyperon.demo.motor.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.decerto.hyperon.demo.motor.context.MotorContext;
import pl.decerto.hyperon.demo.motor.domain.Address;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.demo.motor.domain.Discount;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.demo.motor.domain.Option;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.demo.motor.domain.Vehicle;
import pl.decerto.hyperon.runtime.core.HyperonContext;
import pl.decerto.hyperon.runtime.core.HyperonEngine;
import pl.decerto.hyperon.runtime.model.HyperonDomainAttribute;
import pl.decerto.hyperon.runtime.model.HyperonDomainObject;

@Component
public class Motor {

	private static final Logger log = LoggerFactory.getLogger(Motor.class);

	private final HyperonEngine engine;

	private final QuoteValidator quoteValidator;

	@Autowired
	public Motor(HyperonEngine engine, QuoteValidator quoteValidator) {
		this.engine = engine;
		this.quoteValidator = quoteValidator;
	}


	/*
		====================================================
			 get available coverages for plan FULL
		====================================================
	 */

	public Quote createQuote() {
		log.info("begin ======== CREATE QUOTE ========");

		// basic configuration:
		//  1. plan
		//  2. options defined for this plan
		//  3. coverages defined for this plan

		// 1. obtain FULL plan handle
		HyperonDomainObject plan = engine.getDomain("DEMO", "/PLANS[FULL]");

		// 2. all rating options
		List<HyperonDomainObject> options = plan.getChildren("OPTIONS");

		// 3. all coverages for this plan
		List<HyperonDomainObject> coverages = plan.getChildren("COVERAGES");

		// 4. create quote
		Quote quote = buildQuote(plan, options, coverages);

		log.info("end   ======== CREATE QUOTE ======== \n");

		return quote;
	}

	public void calculate(Quote quote) {

		quoteValidator.validateQuote(quote);
		String planCode = quote.getPlan();

		// handle to plan
		HyperonDomainObject plan = engine.getDomain("DEMO", "/").getChild("PLANS", planCode);

		// for each option and each coverage
		for (Option option : quote.getOptions()) {

			rebuildCoverages(plan.getChildren("COVERAGES"), option);
			// calculate premium for each coverage
			for (Coverage coverage : option.getCoverages()) {

				// coverage definition handle
				HyperonDomainObject coverDef = plan.getChild("COVERAGES", coverage.getCode());

				// dynamic context - derives all paths from current coverage
				MotorContext ctx = new MotorContext(coverage);

				// get LIMIT_1 and LIMIT_2 attributes

				BigDecimal limit1 = coverDef.getAttrDecimal("LIMIT_1", ctx);
				BigDecimal limit2 = coverDef.getAttrDecimal("LIMIT_2", ctx);

				coverage.setLimit1(limit1);
				coverage.setLimit2(limit2);

				// get PREMIUM attribute
				BigDecimal premium = coverDef.getAttr("PREMIUM").getDecimal(ctx);
				coverage.setPremium(premium);
			}

			MotorContext ctx = new MotorContext(option);

			// check discounts
			option.clearDiscounts();
			List<HyperonDomainObject> discounts = plan.getChildren("DISCOUNTS");
			for (HyperonDomainObject discount : discounts) {

				if (discount.getAttrBoolean("available", ctx)) {
					BigDecimal value = discount.getAttr("value").getDecimal(ctx);
					int position = discount.getAttr("position").intValue(ctx);
					option.addDiscount(new Discount(discount.getCode(), discount.getName(), value, position));
				}
			}

		}

		log.info("quote after calculation: {}", quote.print());
	}

	/**
	 * @param plan      rating plan
	 * @param options   all options for this plan
	 * @param coverages all coverages fot this plan
	 * @return constructed quote with coverages for all options
	 */
	private Quote buildQuote(HyperonDomainObject plan, List<HyperonDomainObject> options, List<HyperonDomainObject> coverages) {

		// sample driver's data
		Address adr = new Address("Lake Jackson", "Allwood St", "77566");
		Driver driver = new Driver()
			.setFirstName("John")
			.setLastName("Potter")
			.setGender("M")
			.setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()))
			.setLicenceObtainedAtAge(18)
			.setAddress(adr);

		Vehicle vehicle = new Vehicle();
		vehicle.setProductionYear(2010);
		vehicle.setMakeId(217);
		vehicle.setTypeId(28654);
		vehicle.setModelId(218915);

		Quote q = new Quote(plan.getCode(), driver);
		q.setVehicle(vehicle);


		for (HyperonDomainObject o : options) {

			Option option = new Option(o.getCode(), o.getAttr("ORDER").intValue(new HyperonContext()));
			q.addOption(option);

			rebuildCoverages(coverages, option);
		}
		return q;
	}

	private void rebuildCoverages(List<HyperonDomainObject> coverages, Option option) {
		for (HyperonDomainObject c : coverages) {

			HyperonContext ctx = new HyperonContext(
				"option.code", option.getCode(),
				"coverage.code", c.getCode()
			);

			// get IS_AVAILABLE attribute's value
			boolean isAvailable = c.getAttrBoolean("IS_AVAILABLE", ctx);

			Optional<Coverage> optionCoverage = option.getCoverages()
				.stream()
				.filter(cov -> cov.getCode().equals(c.getCode()))
				.findFirst();

			// add coverage only if available for this option
			if (isAvailable && !optionCoverage.isPresent()) {
				Coverage cover = new Coverage(c.getCode(), c.getName());
				cover.setPosition(c.getAttr("POSITION").intValue(ctx));
				HyperonDomainAttribute description = c.getAttr("DESCRIPTION");
				cover.setDescription(description != null ? description.getString(ctx) : null);
				option.addCoverage(cover);
			}

			// remove not available existing coverage
			if (!isAvailable && optionCoverage.isPresent()) {
				option.getCoverages().remove(optionCoverage.get());
			}
		}

		// sort coverages according to position attribute
		option.getCoverages().sort(Comparator.comparingInt(Coverage::getPosition));

	}
}
