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
import pl.decerto.higson.demo.property.domain.Section;
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

	private static final String COLLECTION_SECTIONS = "SECTIONS";
	private static final String COLLECTION_QUESTIONS = "QUESTIONS";
	private static final String COLLECTION_COVERS = "COVERS";
	private static final String COLLECTION_VARIANTS = "VARIANTS";

	private static final String PARAM_REGIME = "demo.property.apk.regime";
	private static final String PARAM_QUESTION = "demo.property.apk.question";
	private static final String PARAM_COVER_AVAILABILITY = "demo.property.cover.availability";

	private static final String FUNCTION_BUILDING_AGE = "demo.property.util.buildingAge";
	private static final String FUNCTION_ZONE = "demo.property.zone.resolve";
	private static final String FUNCTION_SUMS = "demo.property.sum.suggest";
	private static final String FUNCTION_RISK = "demo.property.risk.factor";
	private static final String FUNCTION_LIMIT = "demo.property.limit.effective";

	private static final String FLOW_QUOTE = "demo.property.calculateQuote";
	private static final String PARAM_APK_ANSWER = "demo.property.apk.answer";
	private static final String PARAM_APK_BLOCKER = "demo.property.apk.question.blocker";
	private static final String PARAM_VALUE_GAP = "demo.property.sum.valueGap";
	private static final String PARAM_APK_REASON = "demo.property.apk.recommendation.reason";
	private static final String PARAM_RATE_PER_SQM = "demo.property.sum.ratePerSqm";
	private static final String PARAM_FLOOD_AVAILABILITY = "demo.property.flood.availability";
	private static final String PARAM_FLOOD_WAITING = "demo.property.flood.waitingPeriod";
	private static final String PARAM_OFFER_TERMS = "demo.property.offer.terms";
	private static final String PARAM_OFFER_CHOICE = "demo.property.offer.choice";
	private static final String PARAM_PERIOD = "demo.property.factor.period";

	private static final String PATH_PERIOD_MONTHS = "quote.policy.periodMonths";

	private static final String OUTCOME_ABSENT = "BUDGET_ABSENT";
	private static final String OUTCOME_FIT = "BUDGET_FIT";
	private static final String OUTCOME_EXCEEDED = "BUDGET_EXCEEDED";
	private static final String REASON_ALTERNATIVE = "BUDGET_ALTERNATIVE";
	private static final String REASON_NO_ALTERNATIVE = "BUDGET_NO_ALTERNATIVE";

	private static final String PATH_BUDGET = "quote.needs.budget";
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
	 * ⚠️ Widoczność sekcji i nieistnienie sekcji to DWA RÓŻNE mechanizmy. Pierwszy to reguła
	 * (`demo.property.apk.section.visibility`), drugi to wersja regionu. Zlanie ich w jedno
	 * odebrałoby demonstracji połowę sensu.
	 */
	private void loadQuestions(PropertyQuote quote) {
		DomainObject product = engine.getDomain(PROFILE, PRODUCT_PATH);
		if (product == null) {
			return;
		}
		List<Section> sections = new ArrayList<>();
		List<Question> questions = new ArrayList<>();

		for (DomainObject branch : product.getChildren(COLLECTION_SECTIONS)) {
			PropertyContext sectionCtx = new PropertyContext(quote).withSection(branch.getCode());

			Section section = new Section();
			section.setCode(branch.getCode());
			section.setTitle(attrString(branch, "TITLE", sectionCtx));
			// FORM albo KNOWN — o to pytamy klienta, albo to już wiemy. Rodzaj jest atrybutem
			// sekcji, więc przeniesienie charakterystyki przedmiotu poza ankietę było zmianą
			// konfiguracji, nie wdrożeniem.
			section.setKind(attrString(branch, "KIND", sectionCtx));
			section.setPosition(intOrZero(branch.getAttrInteger("POSITION", sectionCtx)));
			section.setVisible(Boolean.TRUE.equals(branch.getAttrBoolean("IS_VISIBLE", sectionCtx)));
			sections.add(section);

			for (DomainObject definition : branch.getChildren(COLLECTION_QUESTIONS)) {
				questions.add(readQuestion(quote, definition, branch.getCode()));
			}
		}

		sections.sort(Comparator.comparingInt(Section::getPosition));
		questions.sort(Comparator.comparingInt(Question::getPosition));
		applyReveal(quote, questions);
		quote.setSections(sections);
		quote.setQuestions(questions);
	}

	/** Jedno pytanie. Sekcję niesie GAŁĄŹ, w której pytanie wisi — nie jego własny atrybut. */
	private Question readQuestion(PropertyQuote quote, DomainObject definition, String sectionCode) {
		// W-22: nawigacja po domenie nie wstrzykuje kodu elementu do kontekstu, więc atrybut
		// kluczowany question.code nie miałby się z czego rozwiązać. Kontekst budujemy sami.
		PropertyContext ctx = new PropertyContext(quote).withQuestion(definition.getCode());

		Question question = new Question();
		question.setCode(definition.getCode());
		question.setSection(sectionCode);

		// Cztery rzeczy z ELEMENTU — te, które przy każdym pytaniu są inne.
		//
		// ⛔ HELP już nie ma. Podpowiedź pod każdym pytaniem dawała drugi akapit tam, gdzie widz
		// ma przeczytać jedno zdanie, a przy kilkunastu pytaniach robiła z ankiety ścianę tekstu.
		// Treść pytania musi wystarczyć sama — jeśli nie wystarcza, to jest źle zadane pytanie,
		// a nie brakujący przypis.
		question.setText(attrString(definition, "TEXT", ctx));
		// ⛔ NOŚNE. `KIND` niesie i rodzaj kontrolki, i typ wartości — `PropertyContext.convert`
		// konwertuje po nim. Do 3 września 2026 były to dwa atrybuty; rozdzielone dawały cztery
		// kombinacje, z których sensowne były dwie, a pomyłka kończyła się tym, że „400" nie
		// stawało się `BigDecimal` i budżet klienta znikał w drodze do rekomendacji.
		// `NUMBER` rozpadł się na `INTEGER` i `DECIMAL` i to wystarczyło, żeby drugi atrybut
		// przestał być potrzebny.
		question.setKind(attrString(definition, "KIND", ctx));
		question.setContextPath(attrString(definition, "CONTEXT_PATH", ctx));
		question.setVisible(Boolean.TRUE.equals(definition.getAttrBoolean("IS_VISIBLE", ctx)));

		// Reszta z KATALOGU — te, które na elemencie były identycznym wskaźnikiem `$p` i tylko
		// zasłaniały pięć powyższych. Jeden odczyt zamiast sześciu atrybutów i pięciu tabel.
		ParamValue row = engine.get(PARAM_QUESTION, ctx);
		question.setPosition(intOrZero(row.getInteger("position")));
		question.setRequired(Boolean.TRUE.equals(row.getBoolean("required")));
		question.setOptions(split(row.getString("options")));
		question.setOptionLabels(split(row.getString("labels")));
		question.setPrefillSource(row.getString("prefill"));
		question.setRefusalMessage(row.getString("refusal"));
		question.setRevealedBy(row.getString("revealedBy"));

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
	private void applyReveal(PropertyQuote quote, List<Question> questions) {
		Map<String, Question> byCode = new LinkedHashMap<>();
		for (Question question : questions) {
			byCode.put(question.getCode(), question);
		}
		for (Question question : questions) {
			String revealedBy = question.getRevealedBy();
			if (revealedBy == null || revealedBy.isEmpty()) {
				question.setRevealed(true);
				continue;
			}
			Question source = byCode.get(revealedBy);
			Answer answer = quote.getAnswers().get(revealedBy);
			boolean settled = answer != null && answer.isSettled();
			question.setRevealed(settled || source == null || !source.isRequired());
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
		List<Need> needs = ApkAnalysis.deriveNeeds(engine, PARAM_APK_ANSWER,
			quote.getQuestions(), quote.getAnswers(),
			(question, value) -> new PropertyContext(quote).withQuestion(question).withAnswer(value));
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
	}

	/** Miejsce pakietu w drabince, albo `null`, gdy w tej strefie nie ma go wcale. */
	private Integer rankOf(List<Variant> ladder, String code) {
		for (Variant variant : ladder) {
			if (variant.getCode().equals(code)) {
				return variant.getPosition();
			}
		}
		return null;
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

		// ⛔ PAKIET WSKAZUJE TABELA. Java pyta `demo.property.apk.answer`, co oznacza każda
		// udzielona odpowiedź, i zostawia najbogatszy wskazany pakiet — patrz `ApkAnalysis.pickPackage`.
		// Wcześniej stał tu algorytm maksymalizujący pokrycie potrzeb; działał, ale rozmowa
		// o rekomendacji toczyła się wtedy nad tą klasą, a nie nad tabelą.
		//
		// ⚠️ Wskazany pakiet może nie pokrywać wszystkich potrzeb i NIE PODNOSIMY go z tego
		// powodu. Luka jest nazwana niżej, w `NEEDS_PARTIAL` — tak samo jak luka budżetowa.
		Variant best = variantByCode(quote, ApkAnalysis.pickPackage(engine, PARAM_APK_ANSWER,
			quote.getQuestions(), quote.getAnswers(),
			(question, value) -> new PropertyContext(quote).withQuestion(question).withAnswer(value),
			code -> rankOf(ladder, code)));

		// Tabela nie wskazała nic albo wskazała wariant niedostępny w tej strefie — wtedy
		// najuboższy dostępny, bo milczenie nie jest propozycją.
		if (best == null || !ladder.contains(best)) {
			best = ladder.get(0);
		}

		// Propozycją zostaje `best` BEZ WZGLĘDU NA CENĘ — patrz komentarz nad metodą.
		Variant proposed = best;
		BigDecimal budget = decimalAt(quote, PATH_BUDGET);
		if (!quote.getRegime().isRequiresBudget() || budget == null || budget.signum() <= 0) {
			recommendation.setBudgetOutcome(OUTCOME_ABSENT);
		} else if (best.getAnnualPremium().compareTo(budget) <= 0) {
			recommendation.setBudgetOutcome(OUTCOME_FIT);
		} else {
			recommendation.setBudgetOutcome(OUTCOME_EXCEEDED);
			// Najlepiej dopasowany spośród tych, które mieszczą się w oczekiwanej składce.
			// Może nie być żadnego i to też jest odpowiedź — mówimy o tym osobnym zdaniem.
			Variant alternative = null;
			int covered = -1;
			for (Variant variant : ladder) {
				if (variant.getAnnualPremium().compareTo(budget) > 0) {
					continue;
				}
				int score = ApkAnalysis.score(needs, variant.getCovers()::contains).getCovered();
				if (score > covered) {
					alternative = variant;
					covered = score;
				}
			}
			if (alternative != null) {
				recommendation.setAlternativeVariantCode(alternative.getCode());
				recommendation.setAlternativePremium(alternative.getAnnualPremium());
			}
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
		String outcome = recommendation.getBudgetOutcome();
		if (OUTCOME_FIT.equals(outcome)) {
			reasons.add(new ApkAnalysis.Reason("BUDGET_FIT", money(proposed.getAnnualPremium()), money(budget)));
		} else if (OUTCOME_EXCEEDED.equals(outcome)) {
			// Cena PROPONOWANEGO wariantu — a proponowany jest ten najlepiej dopasowany, bo
			// oczekiwana składka nie obniża zakresu. Zdanie mówi, o ile jesteśmy powyżej.
			reasons.add(new ApkAnalysis.Reason("BUDGET_EXCEEDED",
				money(proposed.getAnnualPremium()), money(budget)));
			Variant alternative = variantByCode(quote, recommendation.getAlternativeVariantCode());
			if (alternative == null) {
				reasons.add(new ApkAnalysis.Reason(REASON_NO_ALTERNATIVE, money(budget)));
			} else {
				List<String> gaps = new ArrayList<>();
				for (String cover : ApkAnalysis.score(quote.getNeeds(),
						alternative.getCovers()::contains).getMissing()) {
					gaps.add(nameOf(product, COLLECTION_COVERS, cover));
				}
				reasons.add(new ApkAnalysis.Reason(REASON_ALTERNATIVE,
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
			String gap = engine.get(PARAM_VALUE_GAP, new PropertyContext(quote)
				.withComputed(PATH_VALUE_RATIO, declared.divide(computed, 4, RoundingMode.HALF_UP)))
				.getString("outcome");
			reasons.add(new ApkAnalysis.Reason("VALUE_" + gap, money(declared), money(computed)));
		}

		ParamValue flood = engine.get(PARAM_FLOOD_AVAILABILITY, new PropertyContext(quote));
		if (!Boolean.TRUE.equals(flood.getBoolean("available"))) {
			reasons.add(new ApkAnalysis.Reason("FLOOD_UNAVAILABLE"));
		} else if (Boolean.TRUE.equals(flood.getBoolean("requiresDeductible"))) {
			reasons.add(new ApkAnalysis.Reason("FLOOD_DEDUCTIBLE", money(flood.getBigDecimal("minDeductible"))));
		}
		reasons.add(new ApkAnalysis.Reason("FLOOD_WAITING",
			engine.get(PARAM_FLOOD_WAITING, new PropertyContext(quote)).getInteger("days")));

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

		describeChoice(quote, offer);
	}

	/**
	 * Rek. 9.3 — czy klient wziął wariant, który rekomendowaliśmy, i co to znaczy.
	 *
	 * ⛔ Java liczy tylko KIERUNEK, i to arytmetyką: porównuje pozycje w drabince, które i tak
	 * przyszły z `demo.property.variant.position`. Co ten kierunek znaczy i jakimi słowami się to
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

		String kind = "MATCHES";
		if (!chosen.equals(recommended)) {
			kind = positionOf(quote, chosen) > positionOf(quote, recommended) ? "RICHER" : "POORER";
		}

		ParamValue note = engine.get(PARAM_OFFER_CHOICE, new PropertyContext(quote).withChoice(kind));
		offer.setChoiceKind(kind);
		offer.setChoiceLabel(note.getString("label"));
		offer.setChoiceText(note.getString("text"));
		offer.setChoiceRequiresAcknowledgement(
			Boolean.TRUE.equals(note.getBoolean("requiresAcknowledgement")));

		offer.setUnmetNeeds(unmetBy(quote, chosen));
	}

	/**
	 * Nazwy potrzeb, których wybrany wariant NIE pokrywa.
	 *
	 * ⛔ Punktowanie idzie przez `ApkAnalysis.score`, tę samą metodę, którą liczy się pokrycie
	 * rekomendacji. Drugie, ręczne porównanie list dałoby drugą definicję słowa „pokryta".
	 */
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

	private int positionOf(PropertyQuote quote, String code) {
		for (Variant variant : quote.getVariants()) {
			if (variant.getCode().equals(code)) {
				return variant.getPosition();
			}
		}
		return 0;
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

	/**
	 * Kwota w zapisie polskim.
	 *
	 * ⛔ Formatuje GOSPODARZ, a nie `ApkAnalysis`. Wspólna klasa podstawia argumenty przez
	 * `String.valueOf`, co jest właściwe dla demo komunikacyjnego (angielskie „1381.19"), ale
	 * w polskim dokumencie dawało „2912.65 zł" z kropką — w zdaniu, które klient dostaje
	 * na piśmie. Język jest cechą ekranu, więc i separator dziesiętny jest.
	 */
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
