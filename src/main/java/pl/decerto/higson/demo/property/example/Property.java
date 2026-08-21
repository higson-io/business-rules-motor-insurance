package pl.decerto.higson.demo.property.example;

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
import io.higson.runtime.model.DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pl.decerto.higson.demo.property.context.PropertyContext;
import pl.decerto.higson.demo.property.domain.Answer;
import pl.decerto.higson.demo.property.domain.Cover;
import pl.decerto.higson.demo.property.domain.Need;
import pl.decerto.higson.demo.property.domain.PropertyQuote;
import pl.decerto.higson.demo.property.domain.Question;
import pl.decerto.higson.demo.property.domain.Recommendation;
import pl.decerto.higson.demo.property.domain.Regime;
import pl.decerto.higson.demo.property.domain.Variant;
import pl.decerto.higson.demo.property.domain.Warning;

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

	private static final String FUNCTION_BUILDING_AGE = "demo.property.util.buildingAge";
	private static final String FUNCTION_ZONE = "demo.property.zone.resolve";
	private static final String FUNCTION_SUMS = "demo.property.sum.suggest";
	private static final String FUNCTION_RISK = "demo.property.risk.factor";
	private static final String FUNCTION_LIMIT = "demo.property.limit.effective";

	private static final String FLOW_QUOTE = "demo.property.calculateQuote";
	private static final String FLOW_RECOMMEND = "demo.property.apk.recommend";

	private final HigsonEngine engine;

	public Property(HigsonEngine engine) {
		this.engine = engine;
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
		quote.setApkPremiums(String.join(";", priced));
	}

	/**
	 * Analiza potrzeb. Ten sam flow obsługuje obie wersje regionu — różnicę robią przełączniki
	 * z apk.regime, a nie drugi algorytm.
	 */
	private void recommend(PropertyQuote quote) {
		quote.setApkAnswers(buildAnswerString(quote));

		Map<String, Object> result = engine.flow(FLOW_RECOMMEND, new PropertyContext(quote)).getResult();

		quote.setNeeds(parseNeeds(asString(result.get("needs"))));
		quote.setWarnings(parseWarnings(asString(result.get("warnings"))));
		applyVariantCoverage(quote, asString(result.get("scores")));
		quote.setRecommendation(parseRecommendation(asString(result.get("recommendation")),
			asString(result.get("justification"))));

		if (quote.getSelectedVariant() == null) {
			quote.setSelectedVariant(quote.getRecommendation().getVariantCode());
		}
	}

	/** "KOD=WARTOŚĆ;…" — wielokrotny wybór rozwija się na jedną pozycję na wartość. */
	private String buildAnswerString(PropertyQuote quote) {
		List<String> parts = new ArrayList<>();
		for (Question question : quote.getQuestions()) {
			Answer answer = quote.getAnswers().get(question.getCode());
			if (answer == null || !answer.isAnswered()) {
				continue;
			}
			if (!answer.getValues().isEmpty()) {
				for (String value : answer.getValues()) {
					parts.add(question.getCode() + "=" + value);
				}
			} else {
				parts.add(question.getCode() + "=" + answer.getValue());
			}
		}
		return String.join(";", parts);
	}

	private List<Need> parseNeeds(String raw) {
		List<Need> needs = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return needs;
		}
		for (String entry : raw.split(";")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 3) {
				needs.add(new Need(parts[0], parts[1], Integer.parseInt(parts[2].trim())));
			}
		}
		return needs;
	}

	private List<Warning> parseWarnings(String raw) {
		List<Warning> warnings = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return warnings;
		}
		for (String entry : raw.split(";;")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 4) {
				warnings.add(new Warning(parts[0], Integer.parseInt(parts[1].trim()), parts[2], parts[3]));
			}
		}
		warnings.sort(Comparator.comparingInt(Warning::getSeverity).reversed());
		return warnings;
	}

	/** "KOD|pokryte|wszystkie|brakujące" — materiał na jawne wskazanie różnic (rek. 9.3). */
	private void applyVariantCoverage(PropertyQuote quote, String raw) {
		if (raw == null || raw.isEmpty()) {
			return;
		}
		Map<String, String[]> byCode = new LinkedHashMap<>();
		for (String entry : raw.split(";")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 4) {
				byCode.put(parts[0], parts);
			}
		}
		for (Variant variant : quote.getVariants()) {
			String[] parts = byCode.get(variant.getCode());
			if (parts == null) {
				continue;
			}
			variant.getCovers().clear();
			for (Cover cover : quote.getCovers()) {
				variant.getCovers().add(cover.getCode());
			}
			variant.getCovers().removeAll(split(parts[3]));
		}
	}

	private Recommendation parseRecommendation(String raw, String justification) {
		Recommendation recommendation = new Recommendation();
		if (raw == null || raw.isEmpty()) {
			return recommendation;
		}
		String[] parts = raw.split("\\|", -1);
		recommendation.setVariantCode(parts[0]);
		recommendation.setCoveredNeeds(Integer.parseInt(parts[1].trim()));
		recommendation.setTotalNeeds(Integer.parseInt(parts[2].trim()));
		recommendation.setMissingCovers(split(parts[3]));
		recommendation.setBudgetOutcome(parts[4]);
		recommendation.setJustification(parseJustification(justification));
		return recommendation;
	}

	/** Zdania przychodzą z prefiksem pozycji, żeby dały się posortować bez drugiej listy. */
	private List<String> parseJustification(String raw) {
		List<String> lines = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return lines;
		}
		for (String entry : raw.split(";;")) {
			int separator = entry.indexOf('|');
			lines.add(separator < 0 ? entry : entry.substring(separator + 1));
		}
		return lines;
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
