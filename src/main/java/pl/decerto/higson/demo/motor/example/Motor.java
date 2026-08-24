package pl.decerto.higson.demo.motor.example;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.model.DomainAttribute;
import io.higson.runtime.model.DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.apk.QuestionCatalogue;
import pl.decerto.higson.demo.apk.RegimeReader;
import pl.decerto.higson.demo.motor.context.MotorContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import pl.decerto.higson.demo.motor.domain.Discount;
import pl.decerto.higson.demo.motor.domain.Driver;
import pl.decerto.higson.demo.motor.domain.Quote;
import pl.decerto.higson.demo.motor.domain.Address;
import pl.decerto.higson.demo.motor.domain.Coverage;
import pl.decerto.higson.demo.motor.domain.Option;
import pl.decerto.higson.demo.motor.domain.Vehicle;

@Component
public class Motor {

	private static final Logger log = LoggerFactory.getLogger(Motor.class);

	private static final String PROFILE = "DEMO";
	private static final String COLLECTION_APK_QUESTIONS = "APK_QUESTIONS";

	private static final String PARAM_APK_REGIME = "demo.moto.apk.regime";
	private static final String PARAM_APK_DEFAULT_ANSWER = "demo.moto.apk.question.defaultAnswer";

	private final HigsonEngine engine;

	private final QuoteValidator quoteValidator;

	@Autowired
	public Motor(HigsonEngine engine, QuoteValidator quoteValidator) {
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
		DomainObject plan = engine.getDomain("DEMO", "/PLANS[FULL]");

		// 2. all rating options
		List<DomainObject> options = plan.getChildren("OPTIONS");

		// 3. all coverages for this plan
		List<DomainObject> coverages = plan.getChildren("COVERAGES");

		// 4. create quote
		Quote quote = buildQuote(plan, options, coverages);

		log.info("end   ======== CREATE QUOTE ======== \n");

		return quote;
	}

	public void calculate(Quote quote) {

		quoteValidator.validateQuote(quote);
		String planCode = quote.getPlan();

		// Data polisy steruje wersją regionu. Ustawiana raz na całą wycenę, czyszczona w finally:
		// effective date jest stanem silnika, nie żądania, i niewyczyszczona wyciekłaby na kolejną
		// wycenę w tym samym wątku.
		Date effective = Date.from(quote.getPolicyStartDate()
			.atStartOfDay(ZoneId.systemDefault()).toInstant());
		engine.setEffectiveDate(effective);
		try {
			calculateWithEffectiveDate(quote, planCode);
		} finally {
			engine.clearEffectiveDate();
		}
	}

	/**
	 * Katalog pytań z domeny. Cztery pytania istnieją wyłącznie w drugiej wersji regionu
	 * APK_MOTOR, więc ta lista zmienia długość wraz z datą polisy — i to jest cała demonstracja.
	 */
	private void loadApkQuestions(Quote quote) {
		DomainObject root = engine.getDomain(PROFILE, "/");
		quote.setApkQuestions(QuestionCatalogue.read(root, COLLECTION_APK_QUESTIONS,
			code -> new MotorContext(quote).withQuestion(code)));
	}

	/**
	 * Wypełnia nieudzielone odpowiedzi wartościami demonstracyjnymi z tabeli.
	 *
	 * Wartości są w Higsonie, a nie tutaj, z tego samego powodu co reszta: gdyby siedziały w
	 * Javie, kod musiałby znać kody pytań, a nowe pytanie wymagałoby wdrożenia.
	 */
	private void seedApkDefaults(Quote quote) {
		for (Question question : quote.getApkQuestions()) {
			if (quote.getApkAnswers().containsKey(question.getCode())) {
				continue;
			}
			MotorContext ctx = new MotorContext(quote).withQuestion(question.getCode());
			String value = engine.get(PARAM_APK_DEFAULT_ANSWER, ctx).getString("value");
			if (value == null || value.isEmpty()) {
				continue;
			}
			quote.getApkAnswers().put(question.getCode(), new Answer(value));
		}
	}

	private void calculateWithEffectiveDate(Quote quote, String planCode) {

		// ⛔ Reżim APK czytany PO setEffectiveDate. Wersjonuje go region APK_MOTOR, oś regulacyjna
		// niezależna od taryfowego regionu MOTOR - próg 1 lipca 2027 to termin z §2 uchwały KNF
		// 190/2026, a nie data zmiany cennika.
		quote.setApkRegime(RegimeReader.read(engine, PARAM_APK_REGIME, new MotorContext(quote)));
		loadApkQuestions(quote);
		seedApkDefaults(quote);
		// Drugi odczyt katalogu: widoczność zależy od odpowiedzi, więc przed ich ustaleniem
		// pytania warunkowe pokazałyby się w złym stanie.
		loadApkQuestions(quote);

		// handle to plan
		DomainObject plan = engine.getDomain("DEMO", "/").getChild("PLANS", planCode);

		// for each option and each coverage
		for (Option option : quote.getOptions()) {

			rebuildCoverages(plan.getChildren("COVERAGES"), option, quote.getVehicle());
			// calculate premium for each coverage
			for (Coverage coverage : option.getCoverages()) {

				// coverage definition handle
				DomainObject coverDef = plan.getChild("COVERAGES", coverage.getCode());

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
			List<DomainObject> discounts = plan.getChildren("DISCOUNTS");
			for (DomainObject discount : discounts) {

				if (discount.getAttrBoolean("available", ctx)) {
					BigDecimal value = discount.getAttr("value").getDecimal(ctx);
					int position = discount.getAttr("position").intValue(ctx);
					option.addDiscount(new Discount(discount.getCode(), discount.getName(), value, position));
				}
			}

			option.getDiscounts().sort(Comparator.comparingInt(Discount::getPosition));
		}

		log.info("quote after calculation: {}", quote.print());
	}

	/**
	 * @param plan      rating plan
	 * @param options   all options for this plan
	 * @param coverages all coverages fot this plan
	 * @return constructed quote with coverages for all options
	 */
	private Quote buildQuote(DomainObject plan, List<DomainObject> options, List<DomainObject> coverages) {

		// sample driver's data
		Address adr = createAddress();
		Driver driver = createDriver(adr);
		Vehicle vehicle = createVehicle();
		Quote quote = new Quote(plan.getCode(), driver);
		quote.setVehicle(vehicle);

		for (DomainObject o : options) {
			Option option = new Option(o.getCode(), o.getAttr("ORDER").intValue(new HigsonContext()));
			quote.addOption(option);

			rebuildCoverages(coverages, option, vehicle);
		}
		return quote;
	}

	private static Vehicle createVehicle() {
		Vehicle vehicle = new Vehicle();
		vehicle.setProductionYear(2010);
		vehicle.setMakeId(217);
		vehicle.setMake("TOYOTA");
		vehicle.setTypeId(28654);
		vehicle.setModelId(218915);
		return vehicle;
	}

	private void rebuildCoverages(List<DomainObject> coverages, Option option, Vehicle vehicle) {
		for (DomainObject c : coverages) {
			HigsonContext ctx = getHigsonContext(option, vehicle, c);
			// get IS_AVAILABLE attribute's value
			boolean isAvailable = c.getAttrBoolean("IS_AVAILABLE", ctx);

			Optional<Coverage> optionCoverage = option.getCoverages()
				.stream()
				.filter(cov -> cov.getCode().equals(c.getCode()))
				.findFirst();

			// add/update coverage only if available for this option
			if (isAvailable) {
				 if (optionCoverage.isPresent()) {
				 	// update
					 setCoverData(c, optionCoverage.get(), ctx);
				 } else {
					 // add new
					 Coverage cover = new Coverage(c.getCode());
					 setCoverData(c, cover, ctx);
					 option.addCoverage(cover);
				 }
			}

			// remove not available existing coverage
			if (!isAvailable && optionCoverage.isPresent()) {
				option.getCoverages().remove(optionCoverage.get());
			}
		}

		// sort coverages according to position attribute
		option.getCoverages().sort(Comparator.comparingInt(Coverage::getPosition));
	}

	private static HigsonContext getHigsonContext(Option option, Vehicle vehicle, DomainObject coverage) {
		return new HigsonContext(
				"option.code", option.getCode(),
				"coverage.code", coverage.getCode(),
				"vehicle.make", vehicle.getMake(),
				"vehicle.makeId", vehicle.getMakeId(),
				"option.quote.address.zipCode", option.getQuote().getDriver().getAddress().getZipCode()
		);
	}

	private void setCoverData(DomainObject domainDataCover, Coverage cover, HigsonContext ctx) {
		cover.setName(domainDataCover.getName());
		cover.setPosition(domainDataCover.getAttr("POSITION").intValue(ctx));
		DomainAttribute description = domainDataCover.getAttr("DESCRIPTION");
		cover.setDescription(description != null ? description.getString(ctx) : null);
	}

    private static Address createAddress() {
        return new Address("Lake Jackson", "Allwood St", "77566");
    }

    private static Driver createDriver(Address adr) {
        return new Driver()
                .setFirstName("John")
                .setLastName("Potter")
                .setGender("M")
                .setDateOfBirth(Date.from(LocalDate.now().minusYears(40).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .setLicenceObtainedAtAge(18)
                .setAddress(adr);
    }
}
