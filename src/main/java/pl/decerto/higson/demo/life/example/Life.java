package pl.decerto.higson.demo.life.example;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.engine.core.output.ParamValue;
import io.higson.runtime.flow.FlowResult;
import io.higson.runtime.model.DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pl.decerto.higson.demo.apk.Answer;
import pl.decerto.higson.demo.apk.Question;
import pl.decerto.higson.demo.apk.QuestionCatalogue;
import pl.decerto.higson.demo.apk.ApkAnalysis;
import pl.decerto.higson.demo.apk.Need;
import pl.decerto.higson.demo.apk.Recommendation;
import pl.decerto.higson.demo.apk.RegimeReader;
import pl.decerto.higson.demo.life.context.LifeContext;
import pl.decerto.higson.demo.life.domain.Benefit;
import pl.decerto.higson.demo.life.domain.Insured;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;
import pl.decerto.higson.demo.life.domain.TariffVersion;
import pl.decerto.higson.demo.life.domain.Variant;

/**
 * Orkiestrator profilu LIFE. Cała wycena siedzi w Higsonie — ta klasa buduje kontekst, woła flow
 * i przepisuje wynik do modelu. Nie ma tu ani jednej stawki, progu ani czynnika.
 */
@Component
public class Life {

	private static final Logger log = LoggerFactory.getLogger(Life.class);

	private static final String PROFILE = "LIFE";

	private static final String INDIVIDUAL_PATH = "/PRODUCT_LINES[INDIVIDUAL]";
	private static final String GROUP_PATH = "/PRODUCT_LINES[GROUP]";

	private static final String FLOW_INDIVIDUAL = "demo.life.individual.calculateQuote";
	private static final String FLOW_GROUP = "demo.life.group.calculateQuote";

	private static final String FUNCTION_UNDERWRITING = "demo.life.underwriting.evaluate";
	private static final String FUNCTION_MAX_SUM = "demo.life.individual.maxSumAssured";
	private static final String FUNCTION_WAITING_PERIOD = "demo.life.individual.effectiveWaitingPeriod";
	private static final String FUNCTION_RISK_FACTOR = "demo.life.individual.riskFactor";
	private static final String FUNCTION_RIDER_PREMIUM = "demo.life.individual.rider.premium";

	private static final String COLLECTION_APK_QUESTIONS = "APK_QUESTIONS";

	private static final String PARAM_TARIFF_VERSION = "demo.life.tariff.version";
	private static final String PARAM_APK_REGIME = "demo.life.apk.regime";
	private static final String PARAM_APK_DEFAULT_ANSWER = "demo.life.apk.question.defaultAnswer";

	private static final String PARAM_APK_NEED = "demo.life.apk.need.derive";
	private static final String PARAM_APK_EXCLUSION = "demo.life.apk.exclusion.conflict";
	private static final String PARAM_APK_REASON = "demo.life.apk.recommendation.reason";
	private static final String PARAM_RIDER_AVAILABILITY = "demo.life.individual.rider.availability";
	private static final String FUNCTION_APK_SUM = "demo.life.apk.suggestSum";
	private static final String PARAM_COVER_EQUIVALENT = "demo.life.apk.cover.equivalent";

	private static final String COVER_MAIN = "MAIN_COVER";
	private static final String PATH_BUDGET = "quote.apk.budget";
	private static final String PATH_OBLIGATIONS = "quote.apk.monthlyObligations";
	private static final String PATH_DEPENDANTS = "quote.apk.dependants";
	private static final String PARAM_RIDER_SUMS = "demo.life.individual.rider.sumsOffered";
	private static final String PARAM_BENEFIT_AVAILABILITY = "demo.life.group.benefit.availability";
	private static final String PARAM_BENEFIT_SUM = "demo.life.group.benefit.sumAssured";
	private static final String PARAM_BENEFIT_KIND = "demo.life.group.benefit.benefitKind";
	private static final String PARAM_BENEFIT_POSITION = "demo.life.group.benefit.position";

	private static final String CAUSE_ILLNESS = "ILLNESS";

	private final HigsonEngine engine;

	public Life(HigsonEngine engine) {
		this.engine = engine;
	}

	/**
	 * Buduje pusty wniosek z konfiguracją odczytaną z domeny: jakie umowy dodatkowe istnieją,
	 * jakie warianty i świadczenia. Nazwy i kolejność też pochodzą z Higsona.
	 */
	public LifeQuote createQuote() {
		log.info("begin ======== CREATE LIFE QUOTE ========");

		LifeQuote quote = new LifeQuote();
		seedInsured(quote);
		loadRiders(quote);

		log.info("end   ======== CREATE LIFE QUOTE ======== riders={}", quote.getRiders().size());

		return quote;
	}

	/**
	 * Wypełnia ubezpieczonego danymi demonstracyjnymi - tak samo jak MOTO zaczyna od gotowego
	 * kierowcy. Bez tego pierwszy ekran nie ma wieku ani budowy, a tabele przedziałowe
	 * (matchRequired = true) nie mają czego dopasować i wycena kończy się PARAM_VALUE_NOT_FOUND
	 * zamiast pokazać składkę.
	 */
	private void seedInsured(LifeQuote quote) {
		Insured insured = quote.getInsured();
		insured.setAge(35);
		insured.setHeightCm(178);
		insured.setWeightKg(78);
		insured.setOccupationClass(1);
		insured.setMonthlyIncome(BigDecimal.valueOf(8_000));
	}

	private void loadRiders(LifeQuote quote) {
		DomainObject line = engine.getDomain(PROFILE, INDIVIDUAL_PATH);
		if (line == null) {
			return;
		}
		List<Rider> riders = new ArrayList<>();
		for (DomainObject definition : line.getChildren("RIDERS")) {
			Rider rider = new Rider(definition.getCode(), definition.getName());
			rider.setDescription(definition.getDescription());
			riders.add(rider);
		}
		riders.sort(Comparator.comparingInt(Rider::getPosition));
		quote.setRiders(riders);
	}

	/**
	 * Konfiguracja umów dodatkowych czytana z domeny: kolejność, rodzaj świadczenia i katalog sum.
	 * Osobno od createQuote, bo atrybuty rozwiązują się przez kontekst, a ten wymaga wniosku.
	 */
	private void loadRiderCatalogue(LifeQuote quote) {
		DomainObject line = engine.getDomain(PROFILE, INDIVIDUAL_PATH);
		if (line == null) {
			return;
		}
		for (Rider rider : quote.getRiders()) {
			DomainObject definition = line.getChild("RIDERS", rider.getCode());
			if (definition == null) {
				continue;
			}
			LifeContext ctx = new LifeContext(quote, rider);

			rider.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			rider.setBenefitKind(definition.getAttrString("BENEFIT_KIND", ctx));
			rider.setAvailable(definition.getAttrBoolean("IS_AVAILABLE", ctx));
			rider.setDefaultSum(definition.getAttrDecimal("SUM_DEFAULT", ctx));
			rider.setSums(readSums(ctx));

			if (rider.getSum() == null) {
				rider.setSum(rider.getDefaultSum());
			}
		}
		quote.getRiders().sort(Comparator.comparingInt(Rider::getPosition));
	}

	/**
	 * Katalog sum czytany WPROST z tabeli, nie przez atrybut domeny.
	 *
	 * Kolumna "sums" jest tablicowa, a resolver atrybutu domeny oczekuje wartości skalarnej i kończy
	 * się GettingWrongTypeException: "Expected AbstractHolder but found NumberHolder[]". Tak samo
	 * kończy się ParamValue.get(row, column) - skalarny getter nie rozpakuje komórki tablicowej.
	 * Wiersz trzeba wziąć przez row() i sięgnąć po getBigDecimalArray, jedyny getter świadomy tablic.
	 */
	private List<BigDecimal> readSums(LifeContext ctx) {
		ParamValue value = engine.get(PARAM_RIDER_SUMS, ctx);
		if (value == null || value.isEmpty()) {
			return new ArrayList<>();
		}
		BigDecimal[] sums = value.row(0).getBigDecimalArray("sums");
		return sums == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(sums));
	}

	private int intOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	private void loadGroupCatalogue(LifeQuote quote) {
		DomainObject line = engine.getDomain(PROFILE, GROUP_PATH);
		if (line == null) {
			return;
		}

		List<Variant> variants = new ArrayList<>();
		for (DomainObject definition : line.getChildren("VARIANTS")) {
			Variant variant = new Variant();
			variant.setCode(definition.getCode());
			variant.setName(definition.getName());
			variants.add(variant);
		}
		quote.setVariants(variants);

		List<Benefit> benefits = new ArrayList<>();
		for (DomainObject definition : line.getChildren("BENEFITS")) {
			Benefit benefit = new Benefit();
			benefit.setCode(definition.getCode());
			benefit.setName(definition.getName());
			benefits.add(benefit);
		}
		quote.setBenefits(benefits);
	}

	/**
	 * Wycena. Data początku ochrony jest ustawiana jako effective date silnika, więc region
	 * wybiera wersję taryfy po dacie polisy, a nie po zegarze serwera. Bez tego demonstracja
	 * "ta sama wycena, inna data" nie miałaby jak zadziałać.
	 */
	public void calculate(LifeQuote quote) {
		Date effective = Date.from(quote.getPolicy().getStartDate()
			.atStartOfDay(ZoneId.systemDefault()).toInstant());
		engine.setEffectiveDate(effective);
		try {
			// Po setEffectiveDate, nigdy przed: to region rozstrzyga, która wersja odpowiada.
			loadTariffVersion(quote);

			// ⛔ Reżim i katalog pytań idą przez DRUGI region — APK_LIFE, oś regulacyjna z progiem
			// 1 lipca 2027. Region LIFE niesie oś taryfową z progiem 1 lipca 2026. Ta sama data
			// polisy rozwiązuje obie, niezależnie od siebie.
			quote.setApkRegime(RegimeReader.read(engine, PARAM_APK_REGIME, new LifeContext(quote)));
			loadApkQuestions(quote);
			seedApkDefaults(quote);
			// Drugi odczyt: widoczność zależy od odpowiedzi i od ścieżki.
			loadApkQuestions(quote);

			if (LifeQuote.LINE_GROUP.equals(quote.getLineCode())) {
				calculateGroup(quote);
			} else {
				calculateIndividual(quote);
			}

			// ⛔ Rekomendacja PO wycenie. Dobór porównuje składkę z budżetem klienta (rek. 8.5),
			// więc przed policzeniem ceny nie miałby czego porównać.
			recommend(quote);
		} finally {
			// Effective date jest stanem silnika, nie żądania - niewyczyszczony wyciekłby
			// na kolejną wycenę w tym samym wątku.
			engine.clearEffectiveDate();
		}
	}

	/**
	 * Nazwa obowiązującej wersji taryfy. Jedyny powód, dla którego ta tabela istnieje: bez niej
	 * ekran musiałby znać datę progu, żeby napisać, co obowiązuje — a to postawiłoby drugą kopię
	 * reguły w przeglądarce.
	 */
	private void loadTariffVersion(LifeQuote quote) {
		ParamValue value = engine.get(PARAM_TARIFF_VERSION, new LifeContext(quote));
		TariffVersion version = new TariffVersion();
		version.setCode(value.getString("code"));
		version.setName(value.getString("name"));
		version.setValidFrom(value.getString("validFrom"));
		quote.setTariffVersion(version);
	}

	/**
	 * Analiza potrzeb: potrzeby, konfrontacja z wyłączeniami, dobór i uzasadnienie.
	 *
	 * ⛔ Które kroki się odbywają, rozstrzyga reżim — wersjonowany datą polisy przez region
	 * APK_LIFE, niezależnie od regionu taryfowego.
	 */
	private void recommend(LifeQuote quote) {
		List<Question> questions = quote.getApkQuestions();
		Map<String, Answer> answers = quote.getApkAnswers();

		List<Need> needs = ApkAnalysis.deriveNeeds(engine, PARAM_APK_NEED, questions, answers,
			(question, value) -> new LifeContext(quote).withQuestion(question).withAnswer(value));
		quote.setApkNeeds(needs);

		quote.setApkWarnings(quote.getApkRegime().isRequiresExclusionCheck()
			? ApkAnalysis.checkExclusions(engine, PARAM_APK_EXCLUSION, questions, answers,
				(question, value) -> new LifeContext(quote).withQuestion(question).withAnswer(value))
			: new ArrayList<>());

		quote.setApkRecommendation(propose(quote, needs));
	}

	/**
	 * Rek. 8.2.d — dobór do potrzeb i możliwości.
	 *
	 * ⛔ Dwie ścieżki, dwa kształty propozycji. Grupowa wybiera WARIANT, bo pracownik wybiera
	 * z półki. Indywidualna nie ma czego wybierać — propozycją jest ZESTAW umów dodatkowych,
	 * i dlatego jej `variantCode` zostaje pusty. To nie jest brak danych, to inny produkt.
	 */
	private Recommendation propose(LifeQuote quote, List<Need> needs) {
		Recommendation recommendation = new Recommendation();
		if (needs.isEmpty()) {
			return recommendation;
		}
		boolean group = LifeQuote.LINE_GROUP.equals(quote.getLineCode());

		ApkAnalysis.Score score;
		Variant best = null;
		Variant proposed = null;
		if (group) {
			List<Variant> variants = new ArrayList<>();
			for (Variant variant : quote.getVariants()) {
				if (variant.isAvailable()) {
					variants.add(variant);
				}
			}
			variants.sort(Comparator.comparingInt(Variant::getPosition));
			if (variants.isEmpty()) {
				return recommendation;
			}
			best = variants.get(0);
			int bestCovered = -1;
			for (Variant variant : variants) {
				int covered = ApkAnalysis.score(needs,
					cover -> benefitAvailable(cover, variant.getCode(), quote)).getCovered();
				if (covered > bestCovered) {
					best = variant;
					bestCovered = covered;
				}
			}
			proposed = best;
			BigDecimal budget = budgetOf(quote);
			if (quote.getApkRegime().isRequiresBudget() && budget != null && budget.signum() > 0
					&& best.getPerEmployee() != null
					&& best.getPerEmployee().compareTo(budget) > 0) {
				recommendation.setBudgetOutcome("BUDGET_EXCEEDED");
				proposed = variants.get(0);
				for (Variant variant : variants) {
					if (variant.getPerEmployee() != null
							&& variant.getPerEmployee().compareTo(budget) <= 0) {
						proposed = variant;
					}
				}
			} else if (quote.getApkRegime().isRequiresBudget() && budget != null && budget.signum() > 0) {
				recommendation.setBudgetOutcome("BUDGET_FIT");
			} else {
				recommendation.setBudgetOutcome("BUDGET_ABSENT");
			}
			Variant picked = proposed;
			score = ApkAnalysis.score(needs, cover -> benefitAvailable(cover, picked.getCode(), quote));
			recommendation.setVariantCode(picked.getCode());
		} else {
			score = ApkAnalysis.score(needs, cover -> riderAvailable(cover, quote));
			BigDecimal budget = budgetOf(quote);
			BigDecimal monthly = quote.getPremiumMonthly();
			if (!quote.getApkRegime().isRequiresBudget() || budget == null || budget.signum() <= 0) {
				recommendation.setBudgetOutcome("BUDGET_ABSENT");
			} else if (monthly != null && monthly.compareTo(budget) <= 0) {
				recommendation.setBudgetOutcome("BUDGET_FIT");
			} else {
				recommendation.setBudgetOutcome("BUDGET_EXCEEDED");
			}
		}

		recommendation.setCoveredNeeds(score.getCovered());
		recommendation.setTotalNeeds(score.getTotal());
		recommendation.setMissingCovers(score.getMissing());

		if (quote.getApkRegime().isRequiresJustification()) {
			recommendation.setJustification(ApkAnalysis.justify(engine, PARAM_APK_REASON,
				reasons(quote, group, proposed, best, score, recommendation.getBudgetOutcome()),
				reason -> new LifeContext(quote).withReason(reason)));
		}
		return recommendation;
	}

	/** Powody uzasadnienia: kody i liczby. Ani jednego zdania — te są w tabeli. */
	private List<ApkAnalysis.Reason> reasons(LifeQuote quote, boolean group, Variant proposed,
			Variant best, ApkAnalysis.Score score, String outcome) {

		DomainObject line = engine.getDomain(PROFILE, group ? GROUP_PATH : INDIVIDUAL_PATH);
		List<ApkAnalysis.Reason> reasons = new ArrayList<>();
		reasons.add(new ApkAnalysis.Reason("COVERS_NEEDS", score.getCovered(), score.getTotal()));

		if (!score.getMissing().isEmpty()) {
			List<String> names = new ArrayList<>();
			for (String cover : score.getMissing()) {
				names.add(coverName(line, group, group ? equivalentOf(cover, quote) : cover));
			}
			reasons.add(new ApkAnalysis.Reason("MISSING_COVER", String.join(", ", names)));
		}

		// Suma z sytuacji klienta, nie z cennika. Mnożniki są w tabeli — patrz apk.suggestSum.
		BigDecimal suggested = asDecimal(engine.call(FUNCTION_APK_SUM, new LifeContext(quote)));
		BigDecimal obligations = decimalAt(quote, PATH_OBLIGATIONS);
		if (suggested != null && suggested.signum() > 0 && obligations != null && obligations.signum() > 0) {
			Object dependants = new LifeContext(quote).get(PATH_DEPENDANTS);
			reasons.add(new ApkAnalysis.Reason("SUM_FROM_OBLIGATIONS",
				suggested, obligations, dependants == null ? 0 : dependants));
		}

		BigDecimal budget = budgetOf(quote);
		if ("BUDGET_FIT".equals(outcome)) {
			reasons.add(new ApkAnalysis.Reason("BUDGET_OK",
				group ? proposed.getPerEmployee() : quote.getPremiumMonthly(), budget));
		} else if ("BUDGET_EXCEEDED".equals(outcome)) {
			if (group) {
				reasons.add(new ApkAnalysis.Reason("BUDGET_EXCEEDED",
					best.getPerEmployee(), budget, coverName(line, true, proposed.getCode())));
			} else {
				// ⛔ Osobna treść: w ochronie indywidualnej NIE MA tańszego wariantu do wybrania.
				reasons.add(new ApkAnalysis.Reason("BUDGET_EXCEEDED_SET",
					quote.getPremiumMonthly(), budget));
			}
		}

		if (!quote.getApkWarnings().isEmpty()) {
			reasons.add(new ApkAnalysis.Reason("EXCLUSION_WARNING", quote.getApkWarnings().size()));
		}
		reasons.add(new ApkAnalysis.Reason("REGIME", quote.getApkRegime().getName()));
		return reasons;
	}

	/** Nazwa umowy albo świadczenia. Umowa główna wisi w osobnej kolekcji niż dodatkowe. */
	private String coverName(DomainObject line, boolean group, String code) {
		String[] collections = group
			? new String[] { "VARIANTS", "BENEFITS" }
			: new String[] { "RIDERS", "MAIN_COVER" };
		for (String collection : collections) {
			for (DomainObject child : line.getChildren(collection)) {
				if (child.getCode().equals(code)) {
					return child.getName();
				}
			}
		}
		return code;
	}

	private boolean benefitAvailable(String cover, String variantCode, LifeQuote quote) {
		try {
			return Boolean.TRUE.equals(engine.get(PARAM_BENEFIT_AVAILABILITY,
				new LifeContext(quote).withBenefit(equivalentOf(cover, quote)).withVariant(variantCode))
				.getBoolean("available"));
		} catch (RuntimeException e) {
			return false;
		}
	}

	/**
	 * Jak ta linia nazywa umowę, o którą pyta potrzeba.
	 *
	 * ⛔ Ankieta powstała w słowniku ochrony indywidualnej, a polisa grupowa nazywa śmierć
	 * `DEATH`, nie `MAIN_COVER`. Bez tego przeliczenia potrzeba pokryta przez podstawowe
	 * świadczenie grupowe raportowała się jako niezaspokojona — i kod wychodził do klienta
	 * w zdaniu „Poza zakresem zostaje: MAIN_COVER".
	 *
	 * Które kody się rozjeżdżają, wie tabela. Java wie tylko, że rozjazd bywa.
	 */
	private String equivalentOf(String cover, LifeQuote quote) {
		try {
			ParamValue value = engine.get(PARAM_COVER_EQUIVALENT,
				new LifeContext(quote).withBenefit(cover));
			return value.isEmpty() ? cover : value.getString("code");
		} catch (RuntimeException e) {
			return cover;
		}
	}

	/**
	 * ⛔ Umowa główna jest ZAWSZE w umowie — to ona nią jest. Nie ma jej w tabeli dostępności
	 * umów dodatkowych i nie powinno jej tam być; bez tego wyjątku potrzeba pokryta przez umowę
	 * główną raportowała się jako niezaspokojona.
	 */
	private boolean riderAvailable(String cover, LifeQuote quote) {
		if (COVER_MAIN.equals(cover)) {
			return true;
		}
		try {
			Rider rider = quote.getRider(cover);
			LifeContext ctx = rider == null
				? new LifeContext(quote).withBenefit(cover)
				: new LifeContext(quote, rider);
			return Boolean.TRUE.equals(engine.get(PARAM_RIDER_AVAILABILITY, ctx).getBoolean("available"));
		} catch (RuntimeException e) {
			return false;
		}
	}

	private BigDecimal budgetOf(LifeQuote quote) {
		return decimalAt(quote, PATH_BUDGET);
	}

	private BigDecimal decimalAt(LifeQuote quote, String path) {
		Object value = new LifeContext(quote).get(path);
		return value instanceof BigDecimal ? (BigDecimal) value : null;
	}

	/**
	 * Katalog pytań z domeny. Pięć pytań istnieje wyłącznie w drugiej wersji regionu APK_LIFE,
	 * więc długość tej listy zmienia się wraz z datą polisy.
	 */
	private void loadApkQuestions(LifeQuote quote) {
		DomainObject root = engine.getDomain(PROFILE, "/");
		quote.setApkQuestions(QuestionCatalogue.read(root, COLLECTION_APK_QUESTIONS,
			code -> new LifeContext(quote).withQuestion(code)));
	}

	/** Odpowiedzi demonstracyjne z tabeli — w Higsonie, żeby Java nie znała kodów pytań. */
	private void seedApkDefaults(LifeQuote quote) {
		for (Question question : quote.getApkQuestions()) {
			if (quote.getApkAnswers().containsKey(question.getCode())) {
				continue;
			}
			LifeContext ctx = new LifeContext(quote).withQuestion(question.getCode());
			String value = engine.get(PARAM_APK_DEFAULT_ANSWER, ctx).getString("value");
			if (value == null || value.isEmpty()) {
				continue;
			}
			quote.getApkAnswers().put(question.getCode(), new Answer(value));
		}
	}

	private void calculateIndividual(LifeQuote quote) {
		LifeContext ctx = new LifeContext(quote);

		loadRiderCatalogue(quote);
		readUnderwriting(quote, ctx);
		readRiderConfiguration(quote);

		Map<String, Object> result = engine.flow(FLOW_INDIVIDUAL, ctx).getResult();

		quote.setDecision(asString(result.get("decision")));
		quote.setBinding(Boolean.parseBoolean(asString(result.get("binding"))));
		quote.setPremiumMonthly(asDecimal(result.get("premiumMonthly")));
		quote.setInstalment(asDecimal(result.get("instalment")));
		quote.setPremiumAnnual(asDecimal(result.get("premiumAnnual")));

		BigDecimal ridersPremium = asDecimal(result.get("ridersPremium"));
		log.debug("individual quote: decision={} monthly={} riders={}",
			quote.getDecision(), quote.getPremiumMonthly(), ridersPremium);
	}

	/** Decyzja, komunikat dla klienta i wymóg badań — wszystko z jednej funkcji. */
	private void readUnderwriting(LifeQuote quote, LifeContext ctx) {
		Object raw = engine.call(FUNCTION_UNDERWRITING, ctx);
		if (!(raw instanceof ParamValue)) {
			return;
		}
		ParamValue value = (ParamValue) raw;
		quote.setDecision(value.get(0, "decision"));
		quote.setMessage(value.get(0, "message"));
		quote.setMedicalRequired(Boolean.TRUE.equals(value.get(0, "medicalRequired")));
		// reasonInternal celowo NIE trafia do modelu - to uzasadnienie dla underwritera,
		// nie komunikat dla klienta (D-1).
	}

	/**
	 * Konfiguracja umów dodatkowych: pułap sumy, karencja i składka — wszystko liczone przez
	 * Higsona per umowa.
	 *
	 * Składka pozycji bierze się z tej samej funkcji, którą sumuje `ridersPremium`, a nie z drugiej
	 * formuły tutaj. Do tej pory nie liczył jej nikt: flow zwracał wyłącznie łączną
	 * `ridersPremium`, a Java ją logowała — więc podsumowanie po prawej wypisywało każdą wybraną
	 * umowę za 0,00 zł, a pasek udziałów pokazywał umowę główną na całej szerokości.
	 *
	 * ⚠️ `riskFactor` liczony RAZ i przekazywany dalej, nie per umowa: to czynnik osoby, nie
	 * umowy, a policzenie go siedem razy dałoby siedem szans na rozjazd zaokrągleń.
	 */
	private void readRiderConfiguration(LifeQuote quote) {
		BigDecimal riskFactor = asDecimal(engine.call(FUNCTION_RISK_FACTOR, new LifeContext(quote)));

		for (Rider rider : quote.getRiders()) {
			LifeContext riderCtx = new LifeContext(quote, rider);

			rider.setMaxSum(asDecimal(engine.call(FUNCTION_MAX_SUM, riderCtx)));

			LifeContext waitingCtx = new LifeContext(quote, rider).withClaimCause(CAUSE_ILLNESS);
			BigDecimal days = asDecimal(engine.call(FUNCTION_WAITING_PERIOD, waitingCtx));
			rider.setWaitingPeriodDays(days == null ? null : days.intValue());

			// Niewybrana umowa nie ma składki — null, a nie zero. Zero jest twierdzeniem
			// „kosztuje nic", a to zupełnie co innego niż „nie jest w umowie".
			rider.setPremium(rider.isSelected()
				? asDecimal(engine.call(FUNCTION_RIDER_PREMIUM, riderCtx, riskFactor))
				: null);
		}
	}

	private void calculateGroup(LifeQuote quote) {
		LifeContext ctx = new LifeContext(quote);

		// Katalog czytany przy KAŻDEJ wycenie, nie raz przy tworzeniu sesji: MENTAL_HEALTH_SUPPORT
		// jest elementem wersji 2 regionu, więc to, jakie świadczenia w ogóle istnieją, zależy od
		// effective date ustawionej przed chwilą z daty polisy (D-4).
		loadGroupCatalogue(quote);
		loadGroupBenefits(quote);

		Map<String, Object> result = engine.flow(FLOW_GROUP, ctx).getResult();

		boolean eligible = Boolean.parseBoolean(asString(result.get("eligible")));
		quote.setDecision(eligible ? "ACCEPT" : "DECLINE");
		quote.setBinding(eligible);

		// Obie liczby fazy programu (D-2). W fazie oferty programCost wraca jako null — i ma
		// wracać: bez rozkładu wyborów pracowników rzeczywisty koszt nie istnieje, a zero byłoby
		// twierdzeniem, że program nic nie kosztuje.
		BigDecimal insured = asDecimal(result.get("insuredCount"));
		quote.setInsuredCount(insured == null ? null : insured.intValue());
		quote.setProgramCost(asDecimal(result.get("programCost")));

		Object variants = result.get("variants");
		if (variants instanceof ParamValue) {
			applyVariantPremiums(quote, (ParamValue) variants);
		}
	}

	/**
	 * Zakres świadczeń w wariantach — to, co wypełnia tabelę porównawczą.
	 *
	 * Suma jest relacją świadczenie-wariant, więc czyta się ją po dwóch kluczach naraz i wyłącznie
	 * tam, gdzie tabela dostępności mówi "tak". Bez tej bramki wiersz wildcard w sumAssured wstawiłby
	 * zero i wariant BASIC wyglądałby na taki, który obejmuje wszystko za darmo.
	 */
	private void loadGroupBenefits(LifeQuote quote) {
		for (Benefit benefit : quote.getBenefits()) {
			LifeContext benefitCtx = new LifeContext(quote).withBenefit(benefit.getCode());

			benefit.setBenefitKind(engine.get(PARAM_BENEFIT_KIND, benefitCtx).getString("kind"));
			benefit.setPosition(intOrZero(engine.get(PARAM_BENEFIT_POSITION, benefitCtx).getInteger("position")));

			Map<String, BigDecimal> sums = new LinkedHashMap<>();
			for (Variant variant : quote.getVariants()) {
				LifeContext cellCtx = new LifeContext(quote)
					.withBenefit(benefit.getCode())
					.withVariant(variant.getCode());

				if (Boolean.TRUE.equals(engine.get(PARAM_BENEFIT_AVAILABILITY, cellCtx).getBoolean("available"))) {
					sums.put(variant.getCode(), engine.get(PARAM_BENEFIT_SUM, cellCtx).getBigDecimal("sum"));
				}
			}
			benefit.setSumByVariant(sums);
		}
		quote.getBenefits().sort(Comparator.comparingInt(Benefit::getPosition));
	}

	private void applyVariantPremiums(LifeQuote quote, ParamValue value) {
		for (Variant variant : quote.getVariants()) {
			variant.setAvailable(false);
			variant.setPerEmployee(null);
			variant.setMonthlyTotalIfAll(null);
		}
		for (int row = 0; row < value.size(); row++) {
			String code = value.get(row, "variant");
			for (Variant variant : quote.getVariants()) {
				if (variant.getCode().equals(code)) {
					variant.setAvailable(true);
					variant.setPerEmployee(asDecimal(value.get(row, "perEmployee")));
					variant.setMonthlyTotalIfAll(asDecimal(value.get(row, "monthlyTotalIfAll")));
				}
			}
		}
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
