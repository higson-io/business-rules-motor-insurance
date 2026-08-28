package pl.decerto.higson.demo.property.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.engine.core.output.ParamValue;
import io.higson.runtime.model.DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pl.decerto.higson.demo.property.context.PropertyContext;
import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.property.domain.Cover;
import pl.decerto.higson.demo.apk.ApkAnalysis;
import pl.decerto.higson.demo.apk.Need;
import pl.decerto.higson.demo.property.domain.Offer;
import pl.decerto.higson.demo.property.service.DocumentNumbers;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.apk.Recommendation;
import pl.decerto.higson.demo.apk.Regime;
import pl.decerto.higson.demo.property.domain.Variant;
import pl.decerto.higson.demo.apk.Warning;

/**
 * Orkiestrator profilu PROPERTY. Cała wiedza produktowa siedzi w Higsonie — ta klasa buduje
 * kontekst, woła reguły i przepisuje wynik do modelu. Nie ma tu ani jednej stawki, ani jednego
 * progu i ani jednego kodu pytania.
 *
 * ⛔ Katalog pytań czytany jest przy KAŻDEJ wycenie, po engine.setEffectiveDate — nie raz przy
 * tworzeniu sesji. Siedem pytań istnieje wyłącznie w wersji 2 regionu, więc to, o co w ogóle
 * wolno zapytać, zależy od daty polisy. Odczyt w konstruktorze wniosku sprawiłby, że wersja 2
 * nigdy by się nie pokazała — i wyglądałoby to na błąd konfiguracji, a nie kodu.
 */
@Component
public class Property {

	private static final Logger log = LoggerFactory.getLogger(Property.class);

	private static final String PROFILE = "PROPERTY";
	private static final String PRODUCT_PATH = "/PRODUCTS[HOME]";

	private static final String COLLECTION_QUESTIONS = "QUESTIONS";
	private static final String COLLECTION_COVERS = "COVERS";
	private static final String COLLECTION_VARIANTS = "VARIANTS";

	private static final String PARAM_REGIME = "demo.property.apk.regime";
	private static final String PARAM_DEFAULT_ANSWER = "demo.property.apk.question.defaultAnswer";
	private static final String PARAM_COVER_AVAILABILITY = "demo.property.cover.availability";

	private static final String FUNCTION_BUILDING_AGE = "demo.property.util.buildingAge";
	private static final String FUNCTION_ZONE = "demo.property.zone.resolve";
	private static final String FUNCTION_SUMS = "demo.property.sum.suggest";
	private static final String FUNCTION_RISK = "demo.property.risk.factor";
	private static final String FUNCTION_LIMIT = "demo.property.limit.effective";

	private static final String FLOW_QUOTE = "demo.property.calculateQuote";
	private static final String PARAM_APK_NEED = "demo.property.apk.need.derive";
	private static final String PARAM_APK_EXCLUSION = "demo.property.apk.exclusion.conflict";
	private static final String PARAM_APK_REASON = "demo.property.apk.recommendation.reason";
	private static final String PARAM_RATE_PER_SQM = "demo.property.sum.ratePerSqm";
	private static final String PARAM_FLOOD_AVAILABILITY = "demo.property.flood.availability";
	private static final String PARAM_FLOOD_WAITING = "demo.property.flood.waitingPeriod";
	private static final String PARAM_OFFER_TERMS = "demo.property.offer.terms";
	private static final String PARAM_PERIOD = "demo.property.factor.period";

	private static final String PATH_PERIOD_MONTHS = "quote.policy.periodMonths";

	private static final String OUTCOME_ABSENT = "BUDGET_ABSENT";
	private static final String OUTCOME_FIT = "BUDGET_FIT";
	private static final String OUTCOME_EXCEEDED = "BUDGET_EXCEEDED";

	private static final String PATH_BUDGET = "quote.needs.budget";
	private static final String PATH_AREA = "quote.property.area";
	private static final String PATH_SUM_BUILDING = "quote.sums.building";
	private static final String PATH_SUM_SUGGESTED = "quote.sums.suggestedBuilding";

	private final HigsonEngine engine;
	private final DocumentNumbers numbers;

	public Property(HigsonEngine engine, DocumentNumbers numbers) {
		this.engine = engine;
		this.numbers = numbers;
	}

	/**
	 * Pusty wniosek. Świadomie nie czyta niczego z Higsona — konfiguracja zależy od daty polisy,
	 * a ta może się jeszcze zmienić.
	 */
	public PropertyQuote createQuote() {
		log.info("begin ======== CREATE PROPERTY QUOTE ========");
		return new PropertyQuote();
	}

	public void calculate(PropertyQuote quote) {
		Date effective = Date.from(quote.getPolicy().getStartDate()
			.atStartOfDay(ZoneId.systemDefault()).toInstant());
		engine.setEffectiveDate(effective);
		try {
			loadRegime(quote);
			loadQuestions(quote);
			seedDefaults(quote);

			computeDerived(quote);
			// Drugi odczyt katalogu: widoczność zależy od odpowiedzi i od wieku budynku, więc przed
			// ich ustaleniem pytania warunkowe pokazałyby się w złym stanie.
			loadQuestions(quote);

			loadCovers(quote);
			priceVariants(quote);
			recommend(quote);
			prepareOffer(quote);
		} finally {
			// Effective date jest stanem silnika, nie żądania — niewyczyszczony wyciekłby na
			// kolejną wycenę w tym samym wątku.
			engine.clearEffectiveDate();
		}
	}

	private void loadRegime(PropertyQuote quote) {
		ParamValue value = engine.get(PARAM_REGIME, new PropertyContext(quote));
		Regime regime = new Regime();
		regime.setCode(value.getString("code"));
		regime.setName(value.getString("name"));
		regime.setLegalBasis(value.getString("legalBasis"));
		regime.setRequiresBudget(Boolean.TRUE.equals(value.getBoolean("requiresBudget")));
		regime.setRequiresExclusionCheck(Boolean.TRUE.equals(value.getBoolean("requiresExclusionCheck")));
		regime.setRequiresJustification(Boolean.TRUE.equals(value.getBoolean("requiresJustification")));
		regime.setRequiresVariantDifferences(Boolean.TRUE.equals(value.getBoolean("requiresVariantDifferences")));
		quote.setRegime(regime);
	}

	private void loadQuestions(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Question> questions = new ArrayList<>();
		for (DomainObject definition : product.getChildren(COLLECTION_QUESTIONS)) {
			// W-22: nawigacja po domenie nie wstrzykuje kodu elementu do kontekstu, więc atrybut
			// kluczowany question.code nie miałby się z czego rozwiązać. Kontekst budujemy sami.
			PropertyContext ctx = new PropertyContext(quote).withQuestion(definition.getCode());

			Question question = new Question();
			question.setCode(definition.getCode());
			question.setText(attrString(definition, "TEXT", ctx));
			question.setKind(attrString(definition, "KIND", ctx));
			question.setSection(attrString(definition, "SECTION", ctx));
			question.setHelp(attrString(definition, "HELP", ctx));
			question.setContextPath(attrString(definition, "CONTEXT_PATH", ctx));
			question.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			question.setVisible(Boolean.TRUE.equals(definition.getAttrBoolean("IS_VISIBLE", ctx)));
			question.setRequired(Boolean.TRUE.equals(definition.getAttrBoolean("IS_REQUIRED", ctx)));
			question.setOptions(split(attrString(definition, "OPTIONS", ctx)));
			question.setOptionLabels(split(attrString(definition, "OPTION_LABELS", ctx)));
			question.setPrefillSource(attrString(definition, "PREFILL_SOURCE", ctx));
			question.setRefusalMessage(attrString(definition, "REFUSAL_MESSAGE", ctx));
			questions.add(question);
		}
		questions.sort(Comparator.comparingInt(Question::getPosition));
		quote.setQuestions(questions);
	}

	/**
	 * Wypełnia nieudzielone odpowiedzi wartościami demonstracyjnymi z tabeli.
	 *
	 * Wartości są w Higsonie, a nie tutaj, z tego samego powodu co reszta: gdyby siedziały w Javie,
	 * kod musiałby znać kody pytań, a nowe pytanie wymagałoby wdrożenia.
	 */
	private void seedDefaults(PropertyQuote quote) {
		for (Question question : quote.getQuestions()) {
			Answer answer = quote.getAnswers().get(question.getCode());
			if (answer != null) {
				continue;
			}
			PropertyContext ctx = new PropertyContext(quote).withQuestion(question.getCode());
			String value = engine.get(PARAM_DEFAULT_ANSWER, ctx).getString("value");
			if (value == null || value.isEmpty()) {
				continue;
			}
			Answer seeded = new Answer();
			if ("MULTI".equals(question.getKind())) {
				seeded.setValues(split(value));
			} else {
				seeded.setValue(value);
			}
			quote.getAnswers().put(question.getCode(), seeded);
		}
	}

	private void computeDerived(PropertyQuote quote) {
		quote.setBuildingAge(asInt(engine.call(FUNCTION_BUILDING_AGE, new PropertyContext(quote))));

		ParamValue zone = (ParamValue) engine.call(FUNCTION_ZONE, new PropertyContext(quote));
		quote.getZone().setFlood(zone.get(0, "flood"));
		quote.getZone().setTheft(zone.get(0, "theft"));

		ParamValue sums = (ParamValue) engine.call(FUNCTION_SUMS, new PropertyContext(quote));
		BigDecimal building = asDecimal(sums.get(0, "building"));
		quote.getSums().setSuggestedBuilding(building);
		if (!quote.isBuildingSumOverridden()) {
			quote.getSums().setBuilding(building);
		}
		quote.getSums().setFixtures(asDecimal(sums.get(0, "fixtures")));
		quote.getSums().setContents(asDecimal(sums.get(0, "contents")));
		// Kradzież i powódź nie mają własnej sumy — ubezpieczają to samo mienie, więc dziedziczą
		// sumę ruchomości i murów. To jest reguła cover.sumBasis, tu tylko jej wykonanie.
		quote.getSums().setTheft(quote.getSums().getContents());
		quote.getSums().setFlood(quote.getSums().getBuilding());
		quote.getSums().setAssistance(BigDecimal.ZERO);

		quote.setRiskFactor(asDecimal(engine.call(FUNCTION_RISK, new PropertyContext(quote))));
	}

	private void loadCovers(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Cover> covers = new ArrayList<>();
		for (DomainObject definition : product.getChildren(COLLECTION_COVERS)) {
			PropertyContext ctx = new PropertyContext(quote).withCover(definition.getCode());

			Cover cover = new Cover();
			cover.setCode(definition.getCode());
			cover.setName(definition.getName());
			cover.setDescription(definition.getDescription());
			cover.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			cover.setSumBasis(attrString(definition, "SUM_BASIS", ctx));
			cover.setCapLabel(attrString(definition, "CAP_LABEL", ctx));
			cover.setEffectiveLimit(asDecimal(engine.call(FUNCTION_LIMIT, ctx)));
			cover.setSum(sumFor(quote, definition.getCode()));
			covers.add(cover);
		}
		covers.sort(Comparator.comparingInt(Cover::getPosition));
		quote.setCovers(covers);
	}

	private void priceVariants(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Variant> variants = new ArrayList<>();
		List<String> priced = new ArrayList<>();

		for (DomainObject definition : product.getChildren(COLLECTION_VARIANTS)) {
			PropertyContext ctx = new PropertyContext(quote).withVariant(definition.getCode());

			Variant variant = new Variant();
			variant.setCode(definition.getCode());
			variant.setName(definition.getName());
			variant.setDescription(definition.getDescription());
			variant.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			variant.setLiabilitySum(definition.getAttrDecimal("LIABILITY_SUM", ctx));

			applyCovers(quote, variant);

			Map<String, Object> result = engine.flow(FLOW_QUOTE, ctx).getResult();
			boolean available = "YES".equals(asString(result.get("available")));
			variant.setAvailable(available);

			if (available) {
				variant.setAnnualPremium(asDecimal(result.get("annual")));
				variant.setInstalment(asDecimal(result.get("instalment")));
				applyBreakdown(variant, asString(result.get("breakdown")));
				priced.add(variant.getCode() + "=" + variant.getAnnualPremium());
			} else {
				variant.setUnavailableReason(asString(result.get("reason")));
			}
			variants.add(variant);
		}
		variants.sort(Comparator.comparingInt(Variant::getPosition));
		quote.setVariants(variants);
	}

	/**
	 * Zakres wariantu czytany WPROST z tabeli dostępności, para (pokrycie, wariant).
	 *
	 * ⚠️ Nie da się go wyprowadzić z listy braków zwróconej przez apk.scoreVariants — tamta jest
	 * liczona względem POTRZEB klienta, więc pokrycie, którego klient nie potrzebuje, nie trafia
	 * ani do pokrytych, ani do brakujących. Macierz porównawcza zbudowana na tamtej liście
	 * pokazywałaby ptaszki wszędzie tam, gdzie klient o nic nie prosił.
	 */
	private void applyCovers(PropertyQuote quote, Variant variant) {
		variant.getCovers().clear();
		for (Cover cover : quote.getCovers()) {
			PropertyContext cellCtx = new PropertyContext(quote)
				.withCover(cover.getCode())
				.withVariant(variant.getCode());
			if (Boolean.TRUE.equals(engine.get(PARAM_COVER_AVAILABILITY, cellCtx).getBoolean("available"))) {
				variant.getCovers().add(cover.getCode());
			}
		}
	}

	/**
	 * Analiza potrzeb. Ten sam algorytm obsługuje obie wersje regionu — różnicę robią
	 * przełączniki z apk.regime, a nie drugi kod.
	 *
	 * ⛔ Java chodzi po listach, tabele rozstrzygają. Ani jednego kodu pytania, progu ani zdania
	 * dla klienta nie ma w tym pliku — wszystkie siedzą w `demo.property.apk.*`.
	 */
	private void recommend(PropertyQuote quote) {
		List<Need> needs = ApkAnalysis.deriveNeeds(engine, PARAM_APK_NEED,
			quote.getQuestions(), quote.getAnswers(),
			(question, value) -> new PropertyContext(quote).withQuestion(question).withAnswer(value));
		quote.setNeeds(needs);

		quote.setWarnings(quote.getRegime().isRequiresExclusionCheck()
			? ApkAnalysis.checkExclusions(engine, PARAM_APK_EXCLUSION,
				quote.getQuestions(), quote.getAnswers(),
				(question, value) -> new PropertyContext(quote).withQuestion(question).withAnswer(value))
			: new ArrayList<>());

		quote.setRecommendation(propose(quote, needs));

		// Dopóki nikt nie wybrał sam, pokazywany wariant IDZIE ZA rekomendacją — także wtedy, gdy
		// rekomendacja się zmieniła, bo zmieniła się data polisy albo odpowiedź w ankiecie.
		if (!quote.isVariantChosen()) {
			quote.setSelectedVariant(quote.getRecommendation().getVariantCode());
		}
	}

	/**
	 * Rek. 8.2.d — dobór do potrzeb i możliwości finansowych.
	 *
	 * ⛔ Przy remisie wygrywa NIŻSZY szczebel drabinki, czyli tańszy. A gdy nic nie mieści się
	 * w budżecie, propozycją zostaje najtańszy wariant Z KOMUNIKATEM — milczenie nie jest
	 * propozycją.
	 */
	private Recommendation propose(PropertyQuote quote, List<Need> needs) {
		Recommendation recommendation = new Recommendation();
		List<Variant> ladder = new ArrayList<>();
		for (Variant variant : quote.getVariants()) {
			if (variant.isAvailable() && variant.getAnnualPremium() != null) {
				ladder.add(variant);
			}
		}
		if (ladder.isEmpty() || needs.isEmpty()) {
			return recommendation;
		}
		ladder.sort(Comparator.comparingInt(Variant::getPosition));

		Variant best = ladder.get(0);
		ApkAnalysis.Score bestScore = ApkAnalysis.score(needs, best.getCovers()::contains);
		for (Variant variant : ladder) {
			ApkAnalysis.Score score = ApkAnalysis.score(needs, variant.getCovers()::contains);
			if (score.getCovered() > bestScore.getCovered()) {
				best = variant;
				bestScore = score;
			}
		}

		Variant proposed = best;
		BigDecimal budget = decimalAt(quote, PATH_BUDGET);
		if (!quote.getRegime().isRequiresBudget() || budget == null || budget.signum() <= 0) {
			recommendation.setBudgetOutcome(OUTCOME_ABSENT);
		} else if (best.getAnnualPremium().compareTo(budget) <= 0) {
			recommendation.setBudgetOutcome(OUTCOME_FIT);
		} else {
			recommendation.setBudgetOutcome(OUTCOME_EXCEEDED);
			proposed = ladder.get(0);
			int covered = ApkAnalysis.score(needs, proposed.getCovers()::contains).getCovered();
			for (Variant variant : ladder) {
				int score = ApkAnalysis.score(needs, variant.getCovers()::contains).getCovered();
				if (variant.getAnnualPremium().compareTo(budget) <= 0 && score >= covered) {
					proposed = variant;
					covered = score;
				}
			}
		}

		ApkAnalysis.Score score = ApkAnalysis.score(needs, proposed.getCovers()::contains);
		recommendation.setVariantCode(proposed.getCode());
		recommendation.setCoveredNeeds(score.getCovered());
		recommendation.setTotalNeeds(score.getTotal());
		recommendation.setMissingCovers(score.getMissing());

		if (quote.getRegime().isRequiresJustification()) {
			recommendation.setJustification(ApkAnalysis.justify(engine, PARAM_APK_REASON,
				reasons(quote, proposed, best, score, recommendation.getBudgetOutcome()),
				reason -> new PropertyContext(quote).withReason(reason)));
		}
		return recommendation;
	}

	/**
	 * Powody uzasadnienia: kody i liczby. Ani jednego zdania — te są w
	 * `demo.property.apk.recommendation.reason`, razem z pozycją, która je sortuje.
	 */
	private List<ApkAnalysis.Reason> reasons(PropertyQuote quote, Variant proposed, Variant best,
			ApkAnalysis.Score score, String outcome) {

		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		List<ApkAnalysis.Reason> reasons = new ArrayList<>();

		if (score.getMissing().isEmpty()) {
			reasons.add(new ApkAnalysis.Reason("NEEDS_MATCHED",
				nameOf(product, COLLECTION_VARIANTS, proposed.getCode()), score.getCovered()));
		} else {
			List<String> names = new ArrayList<>();
			for (String cover : score.getMissing()) {
				names.add(nameOf(product, COLLECTION_COVERS, cover));
			}
			reasons.add(new ApkAnalysis.Reason("NEEDS_PARTIAL",
				nameOf(product, COLLECTION_VARIANTS, proposed.getCode()),
				score.getCovered(), score.getTotal(), String.join(", ", names)));
		}

		PropertyContext ctx = new PropertyContext(quote);
		reasons.add(new ApkAnalysis.Reason("SUM_COMPUTED", ctx.get(PATH_AREA),
			engine.get(PARAM_RATE_PER_SQM, new PropertyContext(quote)).getBigDecimal("rate")));

		// Suma podniesiona albo obniżona ręcznie — klient ma wiedzieć, że odszedł od wyliczenia.
		BigDecimal chosen = decimalAt(quote, PATH_SUM_BUILDING);
		BigDecimal suggested = decimalAt(quote, PATH_SUM_SUGGESTED);
		if (chosen != null && suggested != null && chosen.compareTo(suggested) > 0) {
			reasons.add(new ApkAnalysis.Reason("SUM_RAISED"));
		} else if (chosen != null && suggested != null && chosen.compareTo(suggested) < 0) {
			reasons.add(new ApkAnalysis.Reason("SUM_LOWERED"));
		}

		BigDecimal budget = decimalAt(quote, PATH_BUDGET);
		if (OUTCOME_FIT.equals(outcome)) {
			reasons.add(new ApkAnalysis.Reason("BUDGET_FIT", proposed.getAnnualPremium(), budget));
		} else if (OUTCOME_EXCEEDED.equals(outcome)) {
			// Cena wariantu NAJLEPIEJ dopasowanego, nie proponowanego — to jego przekroczenie
			// budżetu jest powodem, dla którego propozycja zeszła niżej.
			reasons.add(new ApkAnalysis.Reason("BUDGET_EXCEEDED", best.getAnnualPremium(), budget,
				nameOf(product, COLLECTION_VARIANTS, proposed.getCode())));
		} else {
			reasons.add(new ApkAnalysis.Reason("BUDGET_ABSENT"));
		}

		ParamValue flood = engine.get(PARAM_FLOOD_AVAILABILITY, new PropertyContext(quote));
		if (!Boolean.TRUE.equals(flood.getBoolean("available"))) {
			reasons.add(new ApkAnalysis.Reason("FLOOD_UNAVAILABLE"));
		} else if (Boolean.TRUE.equals(flood.getBoolean("requiresDeductible"))) {
			reasons.add(new ApkAnalysis.Reason("FLOOD_DEDUCTIBLE", flood.getBigDecimal("minDeductible")));
		}
		reasons.add(new ApkAnalysis.Reason("FLOOD_WAITING",
			engine.get(PARAM_FLOOD_WAITING, new PropertyContext(quote)).getInteger("days")));

		if (!quote.getWarnings().isEmpty()) {
			reasons.add(new ApkAnalysis.Reason("EXCLUSIONS_FOUND", quote.getWarnings().size()));
		}
		reasons.add(new ApkAnalysis.Reason("LEGAL_BASIS", quote.getRegime().getLegalBasis()));
		return reasons;
	}

	/**
	 * Warunki oferty: ważność, termin płatności, treść oświadczenia i to, czy do oferty dołącza
	 * się dokument analizy potrzeb.
	 *
	 * ⛔ Wszystkie cztery przychodzą z `demo.property.offer.terms`, kluczowanej KODEM REŻIMU.
	 * Reżim jest wersjonowany regionem PROPERTY, więc przesunięcie daty polisy przez 1 lipca 2027
	 * przestawia treść oferty — bez wersjonowania tabeli ofertowej i bez drugiego progu w kodzie.
	 *
	 * ⚠️ Numer i daty przelicza się przy każdej wycenie, ale TYLKO dopóki polisa nie jest
	 * wystawiona: po wystawieniu dokument jest już wydany i zmiana odpowiedzi nie ma prawa
	 * przestawić mu numeru ani okresu ochrony.
	 */
	private void prepareOffer(PropertyQuote quote) {
		Offer offer = quote.getOffer();
		if (offer.isIssued()) {
			return;
		}

		ParamValue terms = engine.get(PARAM_OFFER_TERMS, new PropertyContext(quote));
		LocalDate today = LocalDate.now();

		offer.setNumber(offer.getNumber() == null ? numbers.nextOffer(today) : offer.getNumber());
		offer.setIssuedOn(today);
		offer.setValidUntil(today.plusDays(terms.getInteger("validityDays")));
		offer.setPaymentDueOn(today.plusDays(terms.getInteger("paymentDays")));
		offer.setStatement(terms.getString("statement"));
		offer.setNeedsAnalysisAttached(Boolean.TRUE.equals(terms.getBoolean("needsAnalysisAttached")));
	}

	/**
	 * Akceptacja oferty: ten sam dokument dostaje numer polisy i okres ochrony.
	 *
	 * ⛔ Długość okresu bierze się z `demo.property.factor.period`, czyli z tej samej tabeli, która
	 * wycenia czynnik okresu i liczbę rat. Wyliczanie go drugi raz w Javie dałoby dwie prawdy
	 * o tym, jak długo trwa ochrona — i rozjechałyby się przy pierwszej zmianie oferty.
	 */
	public void issuePolicy(PropertyQuote quote) {
		Offer offer = quote.getOffer();
		if (offer.isIssued() || offer.getNumber() == null) {
			return;
		}
		LocalDate from = quote.getPolicy().getStartDate();
		offer.setPolicyNumber(numbers.policyFor(offer.getNumber()));
		offer.setCoverFrom(from);
		offer.setCoverTo(from.plusMonths(periodMonths(quote)).minusDays(1));

		// Zamrożenie: od tej chwili dokument niesie SWOJE liczby, a nie liczby żywej wyceny.
		String code = quote.getSelectedVariant() == null
			? quote.getRecommendation().getVariantCode()
			: quote.getSelectedVariant();
		offer.setVariantCode(code);
		for (Variant variant : quote.getVariants()) {
			if (variant.getCode().equals(code)) {
				offer.setPremium(variant.getAnnualPremium());
			}
		}
	}

	/** Cofnięcie do stanu oferty — demo musi dać się przejść drugi raz, a warunki przeliczyć. */
	public void withdrawPolicy(PropertyQuote quote) {
		Offer offer = quote.getOffer();
		offer.setPolicyNumber(null);
		offer.setCoverFrom(null);
		offer.setCoverTo(null);
		offer.setVariantCode(null);
		offer.setPremium(null);
	}

	private int periodMonths(PropertyQuote quote) {
		Object answered = new PropertyContext(quote).get(PATH_PERIOD_MONTHS);
		if (answered instanceof Number) {
			return ((Number) answered).intValue();
		}
		// Tabela okresu ma wiersz domyślny, więc odpowiedź bez wartości nadal daje sensowny okres.
		Integer months = engine.get(PARAM_PERIOD, new PropertyContext(quote)).getInteger("installments");
		return months == null ? 12 : months * 12;
	}

	/** Nazwa elementu domeny. Kod, którego nie ma w drzewie, zostaje sobą — nie wywala zdania. */
	private String nameOf(DomainObject product, String collection, String code) {
		for (DomainObject child : product.getChildren(collection)) {
			if (child.getCode().equals(code)) {
				return child.getName();
			}
		}
		return code;
	}

	private BigDecimal decimalAt(PropertyQuote quote, String path) {
		Object value = new PropertyContext(quote).get(path);
		return value instanceof BigDecimal ? (BigDecimal) value : null;
	}

	private void applyBreakdown(Variant variant, String raw) {
		variant.getBreakdown().clear();
		if (raw == null || raw.isEmpty()) {
			return;
		}
		for (String line : raw.split(";")) {
			int separator = line.indexOf('=');
			if (separator > 0) {
				variant.getBreakdown().put(line.substring(0, separator),
					new BigDecimal(line.substring(separator + 1)));
			}
		}
	}

	private BigDecimal sumFor(PropertyQuote quote, String coverCode) {
		switch (coverCode) {
			case "BUILDING":
				return quote.getSums().getBuilding();
			case "FIXTURES":
				return quote.getSums().getFixtures();
			case "CONTENTS":
				return quote.getSums().getContents();
			case "THEFT":
				return quote.getSums().getTheft();
			case "FLOOD":
				return quote.getSums().getFlood();
			case "LIABILITY":
				return quote.getSums().getLiability();
			default:
				return null;
		}
	}

	private List<String> split(String raw) {
		if (raw == null || raw.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(raw.split(",")));
	}

	/**
	 * Atrybut, którego może nie być.
	 *
	 * ⚠️ Atrybut LITERAL o pustej wartości NIE POWSTAJE — getAttrString rzuca wtedy NPE głęboko
	 * w silniku, bez nazwy atrybutu w komunikacie. Pytanie wielokrotnego wyboru nie ma ścieżki
	 * kontekstu, więc ten przypadek jest normalny, a nie wyjątkowy.
	 */
	private String attrString(DomainObject definition, String code, PropertyContext ctx) {
		return definition.getAttribute(code) == null ? null : definition.getAttrString(code, ctx);
	}

	private int intOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	private Integer asInt(Object value) {
		BigDecimal decimal = asDecimal(value);
		return decimal == null ? null : decimal.intValue();
	}

	private String asString(Object value) {
		return value == null ? null : value.toString();
	}

	private BigDecimal asDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		}
		String text = value.toString();
		return text.isEmpty() ? null : new BigDecimal(text);
	}
}
