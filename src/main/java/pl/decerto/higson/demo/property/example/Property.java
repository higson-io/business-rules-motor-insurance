package pl.decerto.higson.demo.property.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

	private static final String COLLECTION_APK = "APK";
	private static final String COLLECTION_QUESTIONS = "QUESTIONS";
	private static final String BRANCH_SUBJECT = "SUBJECT";

	/**
	 * Pola przedmiotu ubezpieczenia — w KOLEJNOŚCI, w jakiej stoją na ekranie.
	 *
	 * ⛔ Jedyna lista kodów w tym pliku i jedyne miejsce, w którym aplikacja wie coś o treści
	 * formularza. Do 7 września 2026 były to elementy drzewa domeny, ale zaciemniały je: drzewo
	 * ma być punktem, od którego zaczyna się czytanie PRODUKTU, a metraż i rok budowy nie są
	 * konfiguracją produktu, tylko formularzem. Wszystko poza samą listą — treść pytania, rodzaj
	 * odpowiedzi, ścieżka kontekstu, kolejność, opcje, podpowiedź — nadal siedzi w katalogu
	 * `demo.property.apk.question`, więc zmiana brzmienia pytania to nadal jedna komórka.
	 */
	private static final List<String> SUBJECT_FIELDS = List.of(
		"PROPERTY_KIND", "AREA", "CONSTRUCTION_YEAR", "WALL_MATERIAL", "FLOOR", "RENOVATED",
		"FINISH_STANDARD", "POSTCODE", "OCCUPANCY", "SECURITY", "CLAIMS_3Y", "COVER_PERIOD");
	private static final String APK_ELEMENT = "NEEDS";
	private static final String COLLECTION_COVERS = "COVERS";
	private static final String COLLECTION_VARIANTS = "VARIANTS";

	private static final String PARAM_REGIME = "demo.property.apk.regime";
	private static final String PARAM_QUESTION = "demo.property.apk.question";

	private static final String FUNCTION_BUILDING_AGE = "demo.property.util.buildingAge";
	private static final String FUNCTION_ZONE = "demo.property.zone.resolve";
	private static final String FUNCTION_SUMS = "demo.property.sum.suggest";
	private static final String FLOW_RISK = "demo.property.risk.calculate";
	private static final String FLOW_RECOMMEND = "demo.property.apk.recommend";

	private static final String FLOW_QUOTE = "demo.property.calculateQuote";
	private static final String PARAM_APK_BLOCKER = "demo.property.apk.question.blocker";
	private static final String PARAM_VALUE_GAP = "demo.property.sum.valueGap";
	private static final String PARAM_APK_REASON = "demo.property.apk.recommendation.reason";
	private static final String PARAM_RATE_PER_SQM = "demo.property.sum.ratePerSqm";
	private static final String PARAM_COVER_FLOOD = "demo.property.cover.flood";
	private static final String PARAM_REASON = "demo.property.apk.reason";
	private static final String PARAM_FLOOD_WAITING = "demo.property.flood.waitingPeriod";
	private static final String PARAM_OFFER_TERMS = "demo.property.offer.terms";
	private static final String PARAM_OFFER_CHOICE = "demo.property.offer.choice";
	private static final String FUNCTION_CHOICE_KIND = "demo.property.offer.choiceKind";
	private static final String PARAM_PERIOD = "demo.property.factor.period";

	private static final String PATH_PERIOD_MONTHS = "quote.policy.periodMonths";

	private static final String OUTCOME_FIT = "BUDGET_FIT";
	private static final String OUTCOME_EXCEEDED = "BUDGET_EXCEEDED";

	private static final String PARAM_REVEAL = "demo.property.apk.question.reveal";
	private static final String PARAM_VISIBILITY = "demo.property.apk.question.visibility";
	private static final String PARAM_RECALCULATION = "demo.property.quote.recalculation";
	private static final String PATH_BUDGET = "quote.needs.budget";
	private static final String PATH_SUM_LIABILITY = "quote.sums.liability";
	private static final String PATH_APK_ANSWERS = "quote.apk.answers";
	private static final String PATH_PREMIUM_PREFIX = "quote.premium.";
	private static final String PATH_PROPERTY_VALUE = "quote.needs.propertyValue";
	private static final String PATH_VALUE_RATIO = "quote.sums.valueRatio";
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

	/**
	 * Katalog pytań — czytany przez SEKCJE, bo sekcja jest gałęzią drzewa, a nie wartością
	 * atrybutu na pytaniu.
	 *
	 * ⛔ Ta zmiana kupuje trzy rzeczy naraz, których atrybut dać nie mógł:
	 *
	 * 1. **Dołożenie sekcji to dołożenie gałęzi.** Wcześniej trzeba było dopisać wartość do
	 *    słownika w opisie atrybutu, dodać tytuł w kodzie ekranu i pamiętać o kolejności.
	 * 2. **Sekcja ma własne ustawienia** — tytuł, kolejność i regułę widoczności, tak samo jak
	 *    pytanie. Tytuły sekcji były zaszyte w komponencie Angulara; teraz przychodzą z domeny.
	 * 3. **Wersjonowanie obejmuje całą sekcję.** `NEEDS` i `EXCLUSIONS` są przypięte do wersji 2
	 *    regionu, więc przed 1 lipca 2027 nie ma ich w drzewie WCALE — razem z pytaniami. Przedtem
	 *    znikało siedem pojedynczych pytań, a sekcje trzeba było wygaszać domyślaniem się z
	 *    pustej listy.
	 *
	 * ⚠️ Widoczność pytania i nieistnienie pytania to DWA RÓŻNE mechanizmy. Pierwszy to reguła
	 * (`demo.property.apk.question.visibility`), drugi to wersja regionu. Zlanie ich w jedno
	 * odebrałoby demonstracji połowę sensu.
	 */
	private void loadQuestions(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Question> questions = new ArrayList<>();
		DomainObject apk = product.getChild(COLLECTION_APK, APK_ELEMENT);
		if (apk != null) {
			readInto(quote, apk.getChildren(COLLECTION_QUESTIONS), APK_ELEMENT, questions);
		}
		for (String code : SUBJECT_FIELDS) {
			questions.add(readCatalogueQuestion(quote, code));
		}

		questions.sort(Comparator.comparingInt(Question::getPosition));
		applyReveal(quote, questions);
		quote.setQuestions(questions);
	}

	/**
	 * Pytania z jednej gałęzi. Kod gałęzi jedzie z pytaniem i mówi, DOKĄD ono należy.
	 *
	 * ⛔ Czyta WYŁĄCZNIE gałąź `APK` — pytania analizy potrzeb w rozumieniu rek. 8, stojące
	 * w drzewie razem z regułami, które z odpowiedzi robią potrzeby i rekomendację. Pola
	 * przedmiotu ubezpieczenia nie są elementami drzewa i idą osobną drogą, przez
	 * `readCatalogueQuestion` i listę `SUBJECT_FIELDS`.
	 *
	 * ⛔ PODZIAŁ JEST DZIEDZINOWY, NIE EKRANOWY. Przez chwilę 4 września 2026 gałęzie nazywały się
	 * „Krok 1" i „Krok 2" i wisiały pod kolekcją `STEPS` — czyli drzewo konfiguracji odwzorowywało
	 * pasek kroków aplikacji. To wiąże konfigurację z prezentacją: przestawienie ekranów każe
	 * przenosić elementy domeny, choć biznesowo nic się nie stało. Kolejność i podział ekranów
	 * należą do aplikacji.
	 */
	private void readInto(PropertyQuote quote, List<DomainObject> definitions, String branch,
			List<Question> questions) {

		for (DomainObject definition : definitions) {
			questions.add(readQuestion(quote, definition, branch));
		}
	}

	/**
	 * Pole przedmiotu ubezpieczenia — całe z katalogu, bez elementu domeny.
	 *
	 * Różni się od pytania APK tylko tym, skąd bierze treść, rodzaj i ścieżkę kontekstu:
	 * pytanie APK ma je na elemencie drzewa, pole przedmiotu w trzech kolumnach katalogu.
	 * Wszystko pozostałe czyta ta sama metoda.
	 */
	private Question readCatalogueQuestion(PropertyQuote quote, String code) {
		PropertyContext ctx = new PropertyContext(quote).withQuestion(code);
		ParamValue row = engine.get(PARAM_QUESTION, ctx);

		Question question = new Question();
		question.setCode(code);
		question.setSection(BRANCH_SUBJECT);
		question.setText(row.getString("text"));
		question.setKind(row.getString("kind"));
		question.setContextPath(row.getString("contextPath"));
		question.setVisible(Boolean.TRUE.equals(
			engine.get(PARAM_VISIBILITY, ctx).getBoolean("visible")));
		applyCatalogue(question, row);
		return question;
	}

	/** Jedno pytanie. Przynależność niesie GAŁĄŹ, w której pytanie wisi — nie jego atrybut. */
	private Question readQuestion(PropertyQuote quote, DomainObject definition, String branch) {
		// W-22: nawigacja po domenie nie wstrzykuje kodu elementu do kontekstu, więc atrybut
		// kluczowany question.code nie miałby się z czego rozwiązać. Kontekst budujemy sami.
		PropertyContext ctx = new PropertyContext(quote).withQuestion(definition.getCode());

		Question question = new Question();
		question.setCode(definition.getCode());
		// Kod kolekcji: QUESTIONS albo SUBJECT. Ekran po nim rozdziela krok pierwszy od drugiego.
		question.setSection(branch);

		// Trzy rzeczy z ELEMENTU: nazwa i dwa atrybuty — te, które przy każdym pytaniu są inne.
		//
		// ⛔ HELP już nie ma. Podpowiedź pod każdym pytaniem dawała drugi akapit tam, gdzie widz
		// ma przeczytać jedno zdanie, a przy kilkunastu pytaniach robiła z ankiety ścianę tekstu.
		// Treść pytania musi wystarczyć sama — jeśli nie wystarcza, to jest źle zadane pytanie,
		// a nie brakujący przypis.
		// ⛔ TREŚĆ PYTANIA TO NAZWA ELEMENTU, nie osobny atrybut. Do 4 września 2026 stała
		// w atrybucie `TEXT`, przez co każde pytanie miało dwie wersje siebie — nazwę widoczną
		// w drzewie i tekst widoczny na ekranie. Cztery z dziewiętnastu zdążyły się rozjechać,
		// a rozjazd nie objawiał się niczym: konfigurator poprawiał nazwę i ekran się nie zmieniał.
		question.setText(definition.getName());
		// ⛔ NOŚNE. `KIND` niesie i rodzaj kontrolki, i typ wartości — `PropertyContext.convert`
		// konwertuje po nim. Do 3 września 2026 były to dwa atrybuty; rozdzielone dawały cztery
		// kombinacje, z których sensowne były dwie, a pomyłka kończyła się tym, że „400" nie
		// stawało się `BigDecimal` i budżet klienta znikał w drodze do rekomendacji.
		// `NUMBER` rozpadł się na `INTEGER` i `DECIMAL` i to wystarczyło, żeby drugi atrybut
		// przestał być potrzebny.
		question.setKind(attrString(definition, "KIND", ctx));
		question.setContextPath(attrString(definition, "CONTEXT_PATH", ctx));
		// ⛔ Widoczność czyta się z reguły WPROST, tak samo jak dla pól przedmiotu. Do 8 września
		// 2026 pytanie APK miało atrybut `IS_VISIBLE`, ale na wszystkich siedmiu był to TEN SAM
		// wskaźnik na tę samą tabelę — czyli siedem kopii jednej informacji, zasłaniających dwa
		// atrybuty, które faktycznie różnią pytania od siebie.
		question.setVisible(Boolean.TRUE.equals(
			engine.get(PARAM_VISIBILITY, ctx).getBoolean("visible")));

		// Reszta z KATALOGU — te, które na elemencie były identycznym wskaźnikiem `$p` i tylko
		// zasłaniały pięć powyższych. Jeden odczyt zamiast sześciu atrybutów i pięciu tabel.
		applyCatalogue(question, engine.get(PARAM_QUESTION, ctx));

		// ⛔ DWIE FLAGI, NIE JEDNA — i nie wolno ich tu sklejać. Reguła `IS_VISIBLE` mówi, czy
		// pytanie DOTYCZY tego klienta (piętro nie dotyczy domu); łańcuch `revealedBy` — czy już
		// PORA je zadać. Ekran pokazuje pytania spełniające oba warunki, ale licznik „pytanie 2
		// z 7" liczy tylko po pierwszym. Sklejone w jedno dawałyby licznik, w którym całkowita
		// liczba pytań rośnie z każdą odpowiedzią.
		//
		// Samo odsłonięcie liczy `applyReveal`, po zbudowaniu całej listy — patrz tam.
		return question;
	}

	/**
	 * Które pytania są już odsłonięte przez łańcuch.
	 *
	 * ⛔ Osobny przebieg po CAŁEJ liście, a nie przy czytaniu pojedynczego pytania. Odsłonięcie
	 * zależy od cudzej odpowiedzi i od cudzej obowiązkowości, więc liczone po jednym elemencie
	 * musiałoby zgadywać, czy pytanie wskazane w `revealedBy` zostało już wczytane.
	 *
	 * ⚠️ Pytanie NIEOBOWIĄZKOWE przepuszcza ankietę dalej także wtedy, gdy klient go pominął.
	 * Do 4 września 2026 rolę tę pełniła odmowa („nie chcę podać"), która domykała pytanie bez
	 * odpowiadania na nie; po jej usunięciu z ekranu bez tej gałęzi jedno pominięte pytanie
	 * o oczekiwaną składkę zatrzymywałoby resztę ankiety na zawsze.
	 */
	/** Ustawienia wspólne dla pytania APK i pola przedmiotu — jeden wiersz katalogu. */
	private void applyCatalogue(Question question, ParamValue row) {
		question.setPosition(intOrZero(row.getInteger("position")));
		question.setRequired(Boolean.TRUE.equals(row.getBoolean("required")));
		question.setOptions(split(row.getString("options")));
		question.setOptionLabels(split(row.getString("labels")));
		question.setPrefillSource(row.getString("prefill"));
		question.setRevealedBy(row.getString("revealedBy"));
	}

	private void applyReveal(PropertyQuote quote, List<Question> questions) {
		Map<String, Question> byCode = new LinkedHashMap<>();
		for (Question question : questions) {
			byCode.put(question.getCode(), question);
		}
		for (Question question : questions) {
			String revealedBy = question.getRevealedBy();
			Question source = revealedBy == null || revealedBy.isEmpty() ? null : byCode.get(revealedBy);
			Answer answer = source == null ? null : quote.getAnswers().get(revealedBy);

			// ⛔ ROZSTRZYGA TABELA `demo.property.apk.question.reveal`. Java podaje jej cztery
			// fakty o pytaniu odsłaniającym i nie wie, co z nich wynika. Do 7 września 2026 stało
			// tu wyrażenie, w którym siedziały dwie reguły naraz i obie łatwo było przeoczyć:
			// odmowa liczy się jak odpowiedź, a pytanie nieobowiązkowe przepuszcza łańcuch dalej.
			PropertyContext ctx = new PropertyContext(quote).withReveal(
				source != null,
				answer != null && answer.isAnswered(),
				answer != null && answer.isRefused(),
				source != null && source.isRequired());

			question.setRevealed(
				Boolean.TRUE.equals(engine.get(PARAM_REVEAL, ctx).getBoolean("revealed")));
		}
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
			String value = engine.get(PARAM_QUESTION, ctx).getString("demoAnswer");
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
		// ⛔ ROZSTRZYGA REGUŁA. „Suma podana ręcznie przez klienta wygrywa z wyliczeniem" to
		// decyzja produktowa, nie mechanika — do 7 września 2026 była warunkiem w tej metodzie.
		boolean overwrite = Boolean.TRUE.equals(engine.get(PARAM_RECALCULATION,
			new PropertyContext(quote)).getBoolean("overwriteManualSum"));
		if (overwrite || !quote.isBuildingSumOverridden()) {
			quote.getSums().setBuilding(building);
		}
		quote.getSums().setFixtures(asDecimal(sums.get(0, "fixtures")));
		quote.getSums().setContents(asDecimal(sums.get(0, "contents")));
		// ⛔ Kradzież, powódź i assistance NIE dostają tu sum. Skąd pokrycie bierze sumę,
		// rozstrzyga atrybut `SUM_BASIS` pokrycia, a wylicza `demo.property.cover.sum`.
		// Do 7 września 2026 stały tu trzy przypisania na sztywno i to one były prawdziwą
		// definicją produktu — tabela o tej samej nazwie istniała, ale nikt jej nie czytał.

		// ⛔ CZYNNIK RYZYKA JEST FLOW, nie funkcją. Siedem tabel czynnikowych i łańcuch mnożeń
		// widać na diagramie zamiast czytać z dwudziestu linii Groovy'ego — a że flow nie umie
		// arytmetyki, mnoży `demo.util.multiply`, dla którego BRAK WARTOŚCI ZNACZY JEDEN.
		quote.setRiskFactor(asDecimal(
			engine.flow(FLOW_RISK, new PropertyContext(quote)).getResult().get("factor")));
	}

	private void loadCovers(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Cover> covers = new ArrayList<>();
		for (DomainObject definition : product.getChildren(COLLECTION_COVERS)) {
			// Wariant w kontekście, bo suma gwarancyjna OC bierze się z katalogu WARIANTU,
			// a nie z metrażu mieszkania — bez tego pokrycie OC nie ma jak dostać sumy.
			PropertyContext ctx = new PropertyContext(quote)
				.withCover(definition.getCode())
				.withVariant(shownVariant(quote));

			Cover cover = new Cover();
			cover.setCode(definition.getCode());
			cover.setName(definition.getName());
			cover.setDescription(definition.getDescription());
			cover.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			cover.setCapLabel(attrString(definition, "LIMIT_LABEL", ctx));
			cover.setEffectiveLimit(definition.getAttrDecimal("LIMIT", ctx));
			// ⛔ SUMA POKRYCIA JEST ATRYBUTEM, nie switchem po kodzie. Do 7 września 2026 stał
			// tu `sumFor`, a razem z nim trzy przypisania na sztywno w `computeDerived`:
			// kradzież = suma ruchomości, powódź = suma murów, assistance = zero. To jest kształt
			// produktu, a nie mechanika, więc rozstrzyga go teraz `atrybut `SUM_BASIS` na pokryciu.
			cover.setSum(definition.getAttrDecimal("SUM", ctx));
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
			// ⛔ SUMA GWARANCYJNA MUSI BYĆ W KONTEKŚCIE JUŻ TERAZ. Tabela pokrycia OC kluczuje się
			// nią, żeby stawka mogła być degresywna — a `quote.sums.liability` ustawiało się
			// dopiero po wyborze wariantu, więc każdy pakiet trafiał w pierwszy przedział
			// i degresja po cichu nie działała. Suma należy do pakietu, więc znamy ją tutaj.
			PropertyContext ctx = new PropertyContext(quote).withVariant(definition.getCode());
			ctx.withComputed(PATH_SUM_LIABILITY,
				definition.getAttrDecimal("LIABILITY_SUM", ctx));

			Variant variant = new Variant();
			variant.setCode(definition.getCode());
			variant.setName(definition.getName());
			variant.setDescription(definition.getDescription());
			variant.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));
			variant.setLiabilitySum(definition.getAttrDecimal("LIABILITY_SUM", ctx));

			applyCovers(definition, variant, ctx);

			Map<String, Object> result = engine.flow(FLOW_QUOTE, ctx).getResult();
			// ⛔ Boolean, nie „YES". Flow czyta dostępność TABELĄ WPROST, odkąd zniknęła nakładka
			// `variant.isAvailable`, która nie robiła nic poza odczytem tej samej tabeli i
			// przepisaniem wyniku na łańcuch. Wartość wraca jako `true`/`false` — porównanie
			// z „YES" dawałoby ciche `false` dla każdego wariantu.
			boolean available = Boolean.parseBoolean(asString(result.get("available")));
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
	 * Skład pakietu czytany Z DRZEWA: pokrycia wiszą pod wariantem jako kolekcja `COVERS`.
	 *
	 * ⛔ Do 4 września 2026 rozstrzygała o tym tabela `demo.property.cover.availability` — para
	 * (pokrycie, wariant) w dwudziestu jeden wierszach. Odpowiadała na pytanie „czy X jest w Y",
	 * ale żeby zobaczyć SKŁAD pakietu, trzeba było przeczytać całą tabelę i odfiltrować kolumnę.
	 * W drzewie skład pakietu jest tam, gdzie pakiet: otwiera się wariant i widać, co zawiera.
	 *
	 * ⛔ Element w tej kolekcji nie ma ŻADNEGO atrybutu — jego kod jest odwołaniem do pokrycia
	 * z `/PRODUCTS[HOME]/COVERS`. Gdyby miał atrybuty, stawka albo limit dałyby się ustawić
	 * w dwóch miejscach i zaczęłyby się rozjeżdżać.
	 *
	 * ⚠️ Nie da się go wyprowadzić z listy braków zwróconej przez `ApkAnalysis.score` — tamta jest
	 * liczona względem POTRZEB klienta, więc pokrycie, którego klient nie potrzebuje, nie trafia
	 * ani do pokrytych, ani do brakujących. Macierz porównawcza zbudowana na tamtej liście
	 * pokazywałaby ptaszki wszędzie tam, gdzie klient o nic nie prosił.
	 */
	private void applyCovers(DomainObject definition, Variant variant, PropertyContext ctx) {
		variant.getCovers().clear();
		for (DomainObject member : definition.getChildren(COLLECTION_COVERS)) {
			// ⛔ POKRYCIE NIEDOSTĘPNE W TEJ SYTUACJI NIE LICZY SIĘ JAKO ZAWARTE. Pakiet zawiera
			// je w drzewie, ale klient go nie dostanie — kradzież bez żadnych zabezpieczeń,
			// powódź w strefie najwyższego zagrożenia. Gdyby liczyło się mimo to, rekomendacja
			// raportowałaby pokrytą potrzebę, której nikt nie pokrywa, i to najczęściej właśnie
			// tę, na której klientowi najbardziej zależy.
			DomainObject cover = engine.getDomain(PROFILE, PRODUCT_PATH)
				.getChild(COLLECTION_COVERS, member.getCode());
			if (cover == null || !Boolean.TRUE.equals(cover.getAttrBoolean("IS_AVAILABLE", ctx))) {
				continue;
			}
			variant.getCovers().add(member.getCode());
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
		// ⛔ POTRZEBY LICZY REGUŁA. `demo.property.apk.needs` przechodzi po odpowiedziach, pyta
		// tabelę, co każda znaczy, i rozstrzyga kolizje: gdy dwie wskazują to samo pokrycie,
		// wygrywa wyższa waga. Ta ostatnia reguła stała do 7 września 2026 w `ApkAnalysis`, przez
		// co o treści potrzeby decydowała kolejność pytań na ekranie.
		PropertyContext ctx = new PropertyContext(quote)
			.withComputed(PATH_APK_ANSWERS, ApkAnalysis.serialize(quote.getQuestions(), quote.getAnswers()));
		List<Need> needs = parseNeeds(attrString(apkElement(), "NEEDS", ctx));
		quote.setNeeds(needs);

		// Jedna tabela, jeden przebieg, jedna lista. Wyłączenia OWU stykające się z sytuacją
		// klienta (rek. 8.2.c) i ochrona, której klient sam sobie odmówił, choć jego sytuacja
		// tego wymaga (rek. 8.2.b), miały do 3 września 2026 dwie osobne tabele o identycznym
		// kształcie wyjścia. Jedna tabela z dwoma kolumnami stref robi obie rzeczy i czyta się
		// jak lista tego, co trzeba klientowi powiedzieć — bo tym właśnie jest.
		applyBlocker(quote);

		quote.setRecommendation(propose(quote, needs));

		// Dopóki nikt nie wybrał sam, pokazywany wariant IDZIE ZA rekomendacją — także wtedy, gdy
		// rekomendacja się zmieniła, bo zmieniła się data polisy albo odpowiedź w ankiecie.
		if (!quote.isVariantChosen()) {
			quote.setSelectedVariant(quote.getRecommendation().getVariantCode());
		}

		// ⛔ SUMA GWARANCYJNA OC JEST CECHĄ PAKIETU, nie wyceny — znamy ją dopiero tutaj, bo
		// dopiero tutaj wiadomo, który pakiet jest pokazywany. Do 4 września 2026 tę zależność
		// niósł atrybut `SUM_BASIS` pokrycia z wartością `CATALOGUE`, a front wybierał po nim
		// między sumą pokrycia a sumą gwarancyjną wariantu. Atrybut wskazywał tabelę, która nie
		// mówiła nic ponadto (pozostałe wiersze były martwe — wykonanie siedziało na sztywno
		// w `computeDerived`), więc znikła razem z nią. Suma ląduje teraz w polu, które od
		// zawsze na nią czekało, i pokrycie OC ma po prostu swoją sumę jak każde inne.
		// Suma gwarancyjna OC zależy od pokazywanego wariantu, a wariant znamy dopiero tutaj —
		// więc pokrycia trzeba przeczytać ponownie, tym razem z właściwym wariantem w kontekście.
		loadCovers(quote);
	}


	/**
	 * Obiekt analizy potrzeb z drzewa — to jego pytamy o rekomendację.
	 *
	 * ⛔ APLIKACJA PYTA OBIEKT, a nie woła funkcji po kodzie. Do 8 września 2026 stało tu
	 * `engine.call("demo.property.apk.indicated", ...)` i trzy podobne — kod funkcji był w Javie,
	 * więc drzewo mogło o niej nie wiedzieć i nic by się nie stało. Teraz reguła jest atrybutem
	 * `APK[NEEDS]`: przestawienie wskaźnika w drzewie zmienia to, co liczy aplikacja.
	 */
	private DomainObject apkElement() {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		return product == null ? null : product.getChild(COLLECTION_APK, APK_ELEMENT);
	}

	/**
	 * Wariant, na który patrzy klient: wybrany, a zanim cokolwiek wybrał — rekomendowany.
	 *
	 * ⚠️ Pokrycia czyta się DWA RAZY: raz przed rekomendacją, gdy nie wiadomo jeszcze, który
	 * wariant jest pokazywany, i raz po niej. Pierwszy przebieg zna sumy z metrażu, ale nie zna
	 * sumy gwarancyjnej OC, bo ta wisi na wariancie. Drugi domyka.
	 */
	private String shownVariant(PropertyQuote quote) {
		if (quote.getSelectedVariant() != null) {
			return quote.getSelectedVariant();
		}
		return quote.getRecommendation() == null ? null : quote.getRecommendation().getVariantCode();
	}

	private Variant variantByCode(PropertyQuote quote, String code) {
		if (code == null) {
			return null;
		}
		for (Variant variant : quote.getVariants()) {
			if (code.equals(variant.getCode())) {
				return variant;
			}
		}
		return null;
	}

	/**
	 * Odpowiedź, po której nie ma czego dalej analizować.
	 *
	 * ⛔ Które to odpowiedzi, mówi tabela `demo.property.apk.question.blocker` — dziś jedna:
	 * brak potrzeby ubezpieczenia nieruchomości. Warunek w Javie związałby bramkę z jednym
	 * konkretnym pytaniem, a wtedy przeniesienie jej gdzie indziej wymagałoby wdrożenia.
	 *
	 * ⚠️ Bramka NIE przerywa wyceny. Ekran ma pokazać komunikat i zablokować przejście dalej,
	 * ale prowadzący musi móc cofnąć odpowiedź i zobaczyć, że wszystko wraca — a to działa
	 * tylko wtedy, gdy reszta wyniku i tak jest policzona.
	 */
	private void applyBlocker(PropertyQuote quote) {
		quote.setBlocked(false);
		quote.setBlockMessage(null);
		for (Question question : quote.getQuestions()) {
			Answer answer = quote.getAnswers().get(question.getCode());
			if (!question.isVisible() || !question.isRevealed() || answer == null || !answer.isAnswered()) {
				continue;
			}
			ParamValue row = engine.get(PARAM_APK_BLOCKER, new PropertyContext(quote)
				.withQuestion(question.getCode()).withAnswer(answer.getValue()));
			if (Boolean.TRUE.equals(row.getBoolean("blocks"))) {
				quote.setBlocked(true);
				quote.setBlockMessage(row.getString("message"));
				return;
			}
		}
	}

	/**
	 * Rek. 8.2.d — dobór do potrzeb i do oczekiwanej składki, w tej kolejności.
	 *
	 * ⛔ OCZEKIWANA SKŁADKA NIE JEST FILTREM ZAKRESU. Propozycją zostaje wariant najlepiej
	 * odpowiadający potrzebom, także wtedy, gdy kosztuje więcej, niż klient podał — a to, że
	 * kosztuje więcej, mówimy wprost. Poprzednia wersja obniżała propozycję do najbogatszego
	 * wariantu mieszczącego się w budżecie, przez co zakres wychodzący z analizy potrzeb zależał
	 * od ceny. Rekomendacja wymaga odwrotnie: priorytetem jest zaspokojenie potrzeb, a oczekiwana
	 * składka jest wymaganiem klienta, o którego niespełnieniu trzeba go poinformować.
	 *
	 * ⛔ Tańszy wariant nie znika — jedzie obok jako ALTERNATYWA, razem z listą tego, czego
	 * w nim brakuje. Propozycja i alternatywa, nie podmiana jednej na drugą.
	 *
	 * ⛔ Przy remisie w liczbie pokrytych potrzeb wygrywa NIŻSZY szczebel drabinki, czyli tańszy.
	 */
	private Recommendation propose(PropertyQuote quote, List<Need> needs) {
		Recommendation recommendation = new Recommendation();
		if (quote.getVariants().isEmpty() || needs.isEmpty()) {
			return recommendation;
		}

		// Kontekst dla reguł rekomendacji: odpowiedzi jako jeden łańcuch i składka każdego
		// policzonego pakietu pod własną ścieżką. Reguła nie zna listy pytań ani listy pakietów —
		// jedno i drugie czyta z tego, co tu dostanie, i z drzewa.
		PropertyContext ctx = new PropertyContext(quote)
			.withComputed(PATH_APK_ANSWERS, ApkAnalysis.serialize(quote.getQuestions(), quote.getAnswers()));
		for (Variant variant : quote.getVariants()) {
			if (variant.isAvailable() && variant.getAnnualPremium() != null) {
				ctx.withComputed(PATH_PREMIUM_PREFIX + variant.getCode(), variant.getAnnualPremium());
			}
		}

		// ⛔ CAŁY DOBÓR PAKIETU TO JEDEN DIAGRAM — `demo.property.apk.recommend`. Java pyta o niego
		// raz i dostaje trzy odpowiedzi. Rozgałęzienie na diagramie JEST treścią rek. 8.2.d:
		// pakiet wynika z potrzeb, oczekiwana składka tylko go nazywa, a alternatywy szukamy
		// wyłącznie wtedy, gdy propozycja jej nie mieści — obok niej, nigdy zamiast.
		Map<String, Object> decision = engine.flow(FLOW_RECOMMEND, ctx).getResult();

		Variant proposed = variantByCode(quote, asString(decision.get("indicated")));
		if (proposed == null) {
			return recommendation;
		}
		recommendation.setBudgetOutcome(asString(decision.get("budgetOutcome")));

		Variant alternative = variantByCode(quote, asString(decision.get("alternative")));
		if (alternative != null) {
			recommendation.setAlternativeVariantCode(alternative.getCode());
			recommendation.setAlternativePremium(alternative.getAnnualPremium());
		}

		ApkAnalysis.Score score = ApkAnalysis.score(needs, proposed.getCovers()::contains);
		recommendation.setVariantCode(proposed.getCode());
		recommendation.setCoveredNeeds(score.getCovered());
		recommendation.setTotalNeeds(score.getTotal());
		recommendation.setMissingCovers(score.getMissing());

		if (quote.getRegime().isRequiresJustification()) {
			recommendation.setJustification(ApkAnalysis.justify(engine, PARAM_APK_REASON,
				reasons(quote, proposed, score, recommendation),
				reason -> new PropertyContext(quote).withReason(reason)));
		}
		return recommendation;
	}

	/**
	 * Powody uzasadnienia: kody i liczby. Ani jednego zdania — te są w
	 * `demo.property.apk.recommendation.reason`, razem z pozycją, która je sortuje.
	 */
	private List<ApkAnalysis.Reason> reasons(PropertyQuote quote, Variant proposed,
			ApkAnalysis.Score score, Recommendation recommendation) {

		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		List<ApkAnalysis.Reason> reasons = new ArrayList<>();

		// ⛔ KTÓRE ZDANIE PADA, ROZSTRZYGA TABELA. Java liczy stosunek pokrytych potrzeb do
		// wszystkich — to arytmetyka — i pyta `demo.property.apk.reason`, co z niego wynika.
		// Do 8 września 2026 stał tu `if (missing.isEmpty())`, więc próg „pokrywa wszystko"
		// nie dał się poluzować bez wdrożenia.
		List<String> missingNames = new ArrayList<>();
		for (String cover : score.getMissing()) {
			missingNames.add(nameOf(product, COLLECTION_COVERS, cover));
		}
		String coverage = reasonFor(quote, "COVERAGE", ratio(score.getCovered(), score.getTotal()), null);
		if (coverage != null) {
			reasons.add(new ApkAnalysis.Reason(coverage,
				nameOf(product, COLLECTION_VARIANTS, proposed.getCode()),
				score.getCovered(), score.getTotal(), String.join(", ", missingNames)));
		}

		PropertyContext ctx = new PropertyContext(quote);
		reasons.add(new ApkAnalysis.Reason("SUM_COMPUTED", ctx.get(PATH_AREA),
			engine.get(PARAM_RATE_PER_SQM, new PropertyContext(quote)).getBigDecimal("rate")));

		// Suma podniesiona albo obniżona ręcznie — klient ma wiedzieć, że odszedł od wyliczenia.
		// ⚠️ Tabela ma pasmo tolerancji 0,99–1,01: różnica poniżej procenta nie jest warta zdania
		// w dokumencie. Warunek w Javie mówił „jakakolwiek różnica" i nie dało się tego zmienić.
		BigDecimal chosen = decimalAt(quote, PATH_SUM_BUILDING);
		BigDecimal suggested = decimalAt(quote, PATH_SUM_SUGGESTED);
		String manualSum = reasonFor(quote, "MANUAL_SUM", ratio(chosen, suggested), null);
		if (manualSum != null) {
			reasons.add(new ApkAnalysis.Reason(manualSum));
		}

		BigDecimal budget = decimalAt(quote, PATH_BUDGET);
		String outcome = recommendation.getBudgetOutcome();
		if (OUTCOME_FIT.equals(outcome)) {
			reasons.add(new ApkAnalysis.Reason("BUDGET_FIT", money(proposed.getAnnualPremium()), money(budget)));
		} else if (OUTCOME_EXCEEDED.equals(outcome)) {
			// Cena PROPONOWANEGO wariantu — a proponowany jest ten najlepiej dopasowany, bo
			// oczekiwana składka nie obniża zakresu. Zdanie mówi, o ile jesteśmy powyżej.
			reasons.add(new ApkAnalysis.Reason("BUDGET_EXCEEDED",
				money(proposed.getAnnualPremium()), money(budget)));
			Variant alternative = variantByCode(quote, recommendation.getAlternativeVariantCode());
			String alternativeReason = reasonFor(quote, "BUDGET_ALTERNATIVE", null, alternative != null);
			if (alternative == null) {
				reasons.add(new ApkAnalysis.Reason(alternativeReason, money(budget)));
			} else {
				List<String> gaps = new ArrayList<>();
				for (String cover : ApkAnalysis.score(quote.getNeeds(),
						alternative.getCovers()::contains).getMissing()) {
					gaps.add(nameOf(product, COLLECTION_COVERS, cover));
				}
				reasons.add(new ApkAnalysis.Reason(alternativeReason,
					nameOf(product, COLLECTION_VARIANTS, alternative.getCode()),
					money(alternative.getAnnualPremium()),
					gaps.isEmpty() ? "nic — pokrywa te same potrzeby" : String.join(", ", gaps)));
			}
		} else {
			reasons.add(new ApkAnalysis.Reason("BUDGET_ABSENT"));
		}

		// Rek. 8.2.c — oszacowanie klienta obok sumy z reguł. Zdanie pada także wtedy, gdy obie
		// liczby się zgadzają: „sprawdziliśmy i się zgadza" jest częścią wykazania zgodności.
		BigDecimal declared = decimalAt(quote, PATH_PROPERTY_VALUE);
		BigDecimal computed = decimalAt(quote, PATH_SUM_SUGGESTED);
		if (declared != null && declared.signum() > 0 && computed != null && computed.signum() > 0) {
			// ⛔ KOD ZDANIA CZYTAMY, a nie sklejamy. Do 8 września 2026 stało tu `"VALUE_" + gap`,
			// co wiązało dwa niezależne słowniki konkatenacją: zmiana słowa w kolumnie `outcome`
			// dawała kod bez wiersza w tabeli zdań, czyli puste zdanie i żadnego błędu.
			String valueGap = engine.get(PARAM_VALUE_GAP, new PropertyContext(quote)
				.withComputed(PATH_VALUE_RATIO, declared.divide(computed, 4, RoundingMode.HALF_UP)))
				.getString("reasonCode");
			if (valueGap != null && !valueGap.isEmpty()) {
				reasons.add(new ApkAnalysis.Reason(valueGap, money(declared), money(computed)));
			}
		}

		// ⛔ POKRYCIE, KTÓREGO KLIENT POTRZEBUJE, A NIE MOŻE DOSTAĆ, MÓWI DLACZEGO. Każde pokrycie
		// ma własną tabelę i własny powód odmowy — kradzież bez zabezpieczeń, powódź w strefie
		// najwyższego zagrożenia. Bez tej pętli pokrycie znikało z pakietu po cichu, a klient
		// czytał tylko, że czegoś brakuje, nie dowiadując się, czego od niego wymagamy.
		for (Need need : quote.getNeeds()) {
			DomainObject cover = product.getChild(COLLECTION_COVERS, need.getCoverCode());
			if (cover == null) {
				continue;
			}
			PropertyContext coverCtx = new PropertyContext(quote).withCover(need.getCoverCode());
			if (Boolean.TRUE.equals(cover.getAttrBoolean("IS_AVAILABLE", coverCtx))) {
				continue;
			}
			String refusal = attrString(cover, "UNAVAILABLE_REASON", coverCtx);
			if (refusal != null && !refusal.isEmpty()) {
				reasons.add(new ApkAnalysis.Reason(refusal));
			}
		}

		// ⛔ TABELA DOSTĘPNOŚCI NAZYWA SWÓJ WŁASNY SKUTEK. Ta sama reguła, która mówi, czy pokrycie
		// wolno sprzedać, mówi też, co powiedzieć klientowi, gdy nie wolno — kolumna `reasonCode`.
		// Do 8 września 2026 wybierał to `if` w tej metodzie, a tabela niosła trzecią kopię tekstu
		// w kolumnie, której nikt nie czytał.
		// ⛔ WŁASNA TABELA POKRYCIA. Powódź ma swoją, bo tylko jej warunki zależą od strefy:
		// w Z0 i Z1 sprzedajemy normalnie, w Z2 z franszyzą, w ZX wcale. Każde pokrycie ma taką
		// tabelę, kluczowaną tym, co dla niego naprawdę rozstrzyga.
		ParamValue flood = engine.get(PARAM_COVER_FLOOD, new PropertyContext(quote));
		String floodReason = flood.getString("reason");
		if (floodReason != null && !floodReason.isEmpty()) {
			reasons.add(new ApkAnalysis.Reason(floodReason, money(flood.getBigDecimal("minDeductible"))));
		}
		String waiting = flood.getString("waitingReason");
		if (waiting != null && !waiting.isEmpty()) {
			reasons.add(new ApkAnalysis.Reason(waiting,
				engine.get(PARAM_FLOOD_WAITING, new PropertyContext(quote)).getInteger("days")));
		}

		// ⛔ Bez podstawy prawnej. Uzasadnienie kończyło się zdaniem „Analizę przeprowadziliśmy
		// zgodnie z: Rekomendacje 8 i 9 — uchwała KNF nr 190/2026" — powołaniem się na przepis
		// w notatce, którą czyta klient. Reżim i jego podstawa prawna nadal są w modelu i nadal
		// sterują tym, co wolno zapytać; przestały być zdaniem w dokumencie.
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
		// ⛔ ROZSTRZYGA REGUŁA. Wystawiony dokument ma numer i datę — zmiana odpowiedzi nie ma
		// prawa przestawić mu okresu ani numeru. Czy tak jest, mówi konfiguracja, nie ten `if`.
		boolean frozen = Boolean.TRUE.equals(engine.get(PARAM_RECALCULATION,
			new PropertyContext(quote)).getBoolean("freezeIssuedOffer"));
		if (offer.isIssued() && frozen) {
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

		describeChoice(quote, offer);
	}

	/**
	 * Rek. 9.3 — czy klient wziął wariant, który rekomendowaliśmy, i co to znaczy.
	 *
	 * ⛔ Java liczy tylko KIERUNEK, i to arytmetyką: porównuje pozycje w drabince, które i tak
	 * przyszły z atrybutu `POSITION` pakietu. Co ten kierunek znaczy i jakimi słowami się to
	 * mówi klientowi, rozstrzyga `demo.property.offer.choice` — bo to jest treść dokumentu,
	 * a nie działanie na liczbach.
	 */
	private void describeChoice(PropertyQuote quote, Offer offer) {
		String recommended = quote.getRecommendation().getVariantCode();
		String chosen = quote.getSelectedVariant() == null ? recommended : quote.getSelectedVariant();
		if (recommended == null || chosen == null) {
			offer.setChoiceKind(null);
			return;
		}

		// ⛔ ROZSTRZYGA REGUŁA, nie Java. Do 7 września 2026 stało tu porównanie pozycji obu
		// wariantów — czyli zdanie „klient wybrał szerzej niż rekomendowaliśmy", które trafia do
		// dokumentu oferty (rek. 9.3), rodziło się w kodzie aplikacji.
		String kind = asString(engine.call(FUNCTION_CHOICE_KIND,
			new PropertyContext(quote).withChoiceVariants(recommended, chosen)));
		if (kind == null) {
			offer.setChoiceKind(null);
			return;
		}

		ParamValue note = engine.get(PARAM_OFFER_CHOICE, new PropertyContext(quote).withChoice(kind));
		offer.setChoiceKind(kind);
		offer.setChoiceLabel(note.getString("label"));
		offer.setChoiceText(note.getString("text"));
		offer.setChoiceRequiresAcknowledgement(
			Boolean.TRUE.equals(note.getBoolean("requiresAcknowledgement")));

		offer.setUnmetNeeds(unmetBy(quote, chosen));
	}


	/** Potrzeby, których wybrany wariant nie pokrywa — nazwy pokryć, nie kody. */
	private List<String> unmetBy(PropertyQuote quote, String variantCode) {
		List<String> names = new ArrayList<>();
		for (Variant variant : quote.getVariants()) {
			if (!variant.getCode().equals(variantCode)) {
				continue;
			}
			DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
			ApkAnalysis.Score score = ApkAnalysis.score(quote.getNeeds(), variant.getCovers()::contains);
			for (String cover : score.getMissing()) {
				names.add(nameOf(product, COLLECTION_COVERS, cover));
			}
		}
		return names;
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
		// ⛔ DŁUGOŚĆ OCHRONY CZYTA SIĘ Z TABELI, a nie wylicza z liczby rat. Do 7 września 2026
		// stało tu `installments * 12` z zaszytą dwunastką na wypadek braku wiersza — dwie liczby
		// produktowe w kodzie aplikacji, i to takie, które przy zmianie oferty na półroczną
		// dawałyby cichy błąd zamiast błędu. Tabela ma wiersz domyślny, więc odpowiedź bez
		// wartości nadal daje sensowny okres.
		return intOrZero(engine.get(PARAM_PERIOD, new PropertyContext(quote)).getInteger("coverMonths"));
	}

	/**
	 * Kwota w zapisie polskim.
	 *
	 * ⛔ Formatuje GOSPODARZ, a nie `ApkAnalysis`. Wspólna klasa podstawia argumenty przez
	 * `String.valueOf`, co jest właściwe dla demo komunikacyjnego (angielskie „1381.19"), ale
	 * w polskim dokumencie dawało „2912.65 zł" z kropką — w zdaniu, które klient dostaje
	 * na piśmie. Język jest cechą ekranu, więc i separator dziesiętny jest.
	 */
	/**
	 * Kod zdania, które ma paść — pyta `demo.property.apk.reason`.
	 *
	 * ⛔ Java podaje FAKTY: czego dotyczy pytanie, jaki wyszedł stosunek i czy warunek logiczny
	 * jest spełniony. Nie wie, jaki próg oddziela jedno zdanie od drugiego ani czy w ogóle jakieś
	 * ma paść — pusta odpowiedź znaczy „milczymy" i jest równie dobrą odpowiedzią jak każda inna.
	 */
	private String reasonFor(PropertyQuote quote, String family, BigDecimal ratio, Boolean flag) {
		String code = engine.get(PARAM_REASON,
			new PropertyContext(quote).withReasonTrigger(family, ratio, flag)).getString("reasonCode");
		return code == null || code.isEmpty() ? null : code;
	}

	/** Stosunek dwóch liczb, albo `null`, gdy pytanie nie ma sensu. Sama arytmetyka. */
	private BigDecimal ratio(BigDecimal value, BigDecimal base) {
		if (value == null || base == null || base.signum() == 0) {
			return null;
		}
		return value.divide(base, 4, RoundingMode.HALF_UP);
	}

	private BigDecimal ratio(int value, int base) {
		return base == 0 ? null : ratio(BigDecimal.valueOf(value), BigDecimal.valueOf(base));
	}

	private String money(BigDecimal value) {
		if (value == null) {
			return "—";
		}
		NumberFormat format = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
		format.setMinimumFractionDigits(value.stripTrailingZeros().scale() > 0 ? 2 : 0);
		format.setMaximumFractionDigits(2);
		return format.format(value);
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


	/**
	 * Lista potrzeb z reguły: `POKRYCIE|treść|waga`, rekordy rozdzielone średnikiem.
	 *
	 * ⚠️ Rozdzielamy po PIERWSZYCH dwóch kreskach, nie przez `split` po całości — treść potrzeby
	 * jest zdaniem dla klienta i nie ma powodu, żeby kod aplikacji dyktował, czego w niej nie ma.
	 */
	private List<Need> parseNeeds(String raw) {
		List<Need> needs = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return needs;
		}
		for (String line : raw.split(";")) {
			int cover = line.indexOf('|');
			int weight = line.lastIndexOf('|');
			if (cover < 1 || weight <= cover) {
				continue;
			}
			needs.add(new Need(line.substring(0, cover), line.substring(cover + 1, weight),
				intOrZero(asInt(line.substring(weight + 1)))));
		}
		return needs;
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
