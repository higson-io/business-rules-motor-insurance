package pl.decerto.higson.demo.property.example;

import java.math.BigDecimal;
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
import io.higson.runtime.engine.core.output.MultiValue;
import io.higson.runtime.engine.core.output.ParamValue;
import io.higson.runtime.flow.FlowResult;
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
	private static final String ATTR_TEXT = "TEXT";
	private static final String ATTR_KIND = "KIND";
	private static final String ATTR_CONTEXT_PATH = "CONTEXT_PATH";
	private static final String ATTR_REQUIRED = "REQUIRED";
	private static final String ATTR_POSITION = "POSITION";

	private static final String KIND_GROUP = "GROUP";
	private static final String KIND_BOOLEAN = "BOOLEAN";
	private static final List<String> BOOLEAN_OPTIONS = List.of("true", "false");
	private static final List<String> BOOLEAN_LABELS = List.of("Tak", "Nie");

	private static final String COLLECTION_GROUPS = "GROUPS";

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
		"PROPERTY_KIND", "AREA", "CONSTRUCTION_YEAR", "WALL_MATERIAL", "FLOOR", "DEDUCTIBLE",
		"FINISH_STANDARD", "POSTCODE", "OCCUPANCY", "SECURITY", "CLAIMS_3Y", "PRIOR_FLOOD_COVER",
		"COVER_PERIOD");
	private static final String APK_ELEMENT = "NEEDS";
	private static final String COLLECTION_COVERS = "COVERS";

	/**
	 * ⛔ JEDYNY kod pokrycia w tym pliku i jedyny, jaki musi tu być: franszyza strefowa i karencja
	 * to warunki JEDNEGO pokrycia, a zdanie o nich pada w uzasadnieniu niezależnie od tego, który
	 * pakiet oglądamy. Adresem jest element w drzewie — reguły stoją pod jego atrybutami.
	 */
	private static final String COVER_FLOOD = "FLOOD";
	private static final String COLLECTION_VARIANTS = "VARIANTS";

	/**
	 * Atrybuty, pod którymi wiszą reguły — jedyne adresy, jakimi ta klasa dysponuje.
	 *
	 * ⛔ Do 10 września 2026 stały tu KODY REGUŁ i aplikacja wołała je wprost. Drzewo mogło wtedy
	 * o regule nie wiedzieć, a przestawienie wskaźnika w Studio nie zmieniało niczego — bo Java
	 * i tak szła obok. Teraz adresem jest element i atrybut; na co atrybut wskazuje, rozstrzyga
	 * konfiguracja, a Java nie wie nawet, czy pod spodem jest tabela, funkcja czy flow.
	 */
	private static final String ATTR_SUBJECT_FIELDS = "SUBJECT_FIELDS";
	private static final String ATTR_RECOMMENDATION = "RECOMMENDATION";
	private static final String ATTR_NEEDS_DECLARED = "NEEDS_DECLARED";
	private static final String ATTR_NEEDS_INFERRED = "NEEDS_INFERRED";
	private static final String ATTR_JUSTIFICATION = "JUSTIFICATION";

	private static final String ATTR_ZONES = "ZONES";
	private static final String ATTR_SUM_RATE = "SUM_RATE";
	private static final String ATTR_COVER_PERIOD = "COVER_PERIOD";
	private static final String ATTR_CHOICE_RELATION = "CHOICE_RELATION";

	private static final String ATTR_PREMIUM = "PREMIUM";
	private static final String ATTR_LIABILITY_SUM = "LIABILITY_SUM";
	private static final String ATTR_DEDUCTIBLE = "DEDUCTIBLE";
	private static final String ATTR_WAITING_PERIOD = "WAITING_PERIOD";

	private static final String PATH_PERIOD_MONTHS = "quote.policy.periodMonths";

	private static final String OUTCOME_FIT = "BUDGET_FIT";
	private static final String OUTCOME_EXCEEDED = "BUDGET_EXCEEDED";

	/**
	 * ⛔ WIDOCZNOŚĆ NA PYTANIU, a jego reguła należy do NIEGO. Wartością atrybutu jest `true` —
	 * pytamy zawsze — albo wskazanie własnej tabeli pytania, kluczowanej odpowiedzią, od której
	 * zależy. Tabela nie ma wtedy kolumny z kodem pytania: o kogo chodzi, mówi miejsce w drzewie.
	 *
	 * ⛔ Pola przedmiotu elementami NIE SĄ, więc ich widoczność stoi przy ich katalogu, na
	 * produkcie — i tamta tabela kod pola potrzebuje, bo obsługuje je wszystkie.
	 */
	private static final String ATTR_IS_VISIBLE = "IS_VISIBLE";
	private static final String ATTR_FIELD_VISIBILITY = "FIELD_VISIBILITY";

	private static final String ATTR_RISK_FACTOR = "RISK_FACTOR";
	private static final String ATTR_SUM_INSURED = "SUM_INSURED";
	private static final String ATTR_OFFER_VALIDITY_DAYS = "OFFER_VALIDITY_DAYS";

	private static final String ATTR_OFFER_PAYMENT_DAYS = "OFFER_PAYMENT_DAYS";

	private static final String ATTR_OFFER_NEEDS_ANALYSIS = "OFFER_NEEDS_ANALYSIS";

	private static final String ATTR_OFFER_STATEMENT = "OFFER_STATEMENT";

	private static final String PATH_BUDGET = "quote.needs.budget";
	private static final String PATH_SUM_LIABILITY = "quote.sums.liability";
	private static final String PATH_APK_COVERS = "quote.apk.covers";
	private static final String PATH_APK_PROPOSED = "quote.apk.proposed";
	private static final String PATH_APK_ALTERNATIVE = "quote.apk.alternative";
	private static final String PATH_APK_PROPOSED_PREMIUM = "quote.apk.proposedPremium";
	private static final String PATH_PREMIUM_PREFIX = "quote.premium.";
	private static final String PATH_PROPERTY_VALUE = "quote.needs.propertyValue";
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
	 * ⚠️ Niezadanie pytania i nieistnienie pytania to DWIE RÓŻNE rzeczy. Pierwsze mówi pytanie
	 * samo o sobie (warunek odsłonięcia), drugie — wersja regionu. Zlanie ich w jedno odebrałoby
	 * demonstracji połowę sensu.
	 */
	private void loadQuestions(PropertyQuote quote) {
		DomainObject product = productElement();
		if (product == null) {
			return;
		}
		List<Question> questions = new ArrayList<>();
		DomainObject apk = product.getChild(COLLECTION_APK, APK_ELEMENT);
		if (apk != null) {
			readGroups(quote, apk.getChildren(COLLECTION_GROUPS), questions);
		}
		for (String code : SUBJECT_FIELDS) {
			questions.add(readCatalogueQuestion(quote, code));
		}

		questions.sort(Comparator.comparingInt(Question::getPosition));
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
	private void readGroups(PropertyQuote quote, List<DomainObject> groups, List<Question> questions) {
		for (DomainObject group : groups) {
			Question parent = readQuestion(quote, group, APK_ELEMENT);
			// ⛔ Grupa NIE MA własnej odpowiedzi — `KIND` bierze się stąd, a nie z atrybutu, bo
			// grupa go nie ma. Ekran rozpoznaje po tym, że ma renderować pola, a nie kontrolkę.
			parent.setKind(KIND_GROUP);
			// ⛔ Dla GRUPY treścią jest jej NAZWA, nie atrybut `TEXT` — grupa go nie ma i mieć nie
			// powinna. Nazwa kroku pracuje w dwóch miejscach: jako pozycja w spisie i jako nagłówek,
			// gdy grupa pokazuje więcej niż jedno pytanie. Pytanie ma odwrotnie: krótką nazwę
			// w drzewie i pełne zdanie w atrybucie.
			parent.setText(group.getName());
			questions.add(parent);

			// ⛔ KROK JEST ZAWSZE NA LIŚCIE. Spis kroków ma być stały — prowadzący widzi, z ilu
			// etapów składa się rozmowa, zanim padnie pierwsza odpowiedź. Warunkowość działa
			// WEWNĄTRZ kroku: to pytania pojawiają się i znikają, nie kroki.
			//
			// ⛔ Obowiązkowość natomiast BIERZE SIĘ Z PYTAŃ, a nie z własnego atrybutu grupy:
			// krok trzeba domknąć dokładnie wtedy, gdy któreś z jego pytań jest obowiązkowe,
			// więc atrybut na grupie byłby tą samą regułą zapisaną drugi raz.
			parent.setVisible(true);
			boolean anyRequired = false;
			for (DomainObject definition : group.getChildren(COLLECTION_QUESTIONS)) {
				Question question = readQuestion(quote, definition, APK_ELEMENT);
				anyRequired = anyRequired || question.isRequired();
				// ⛔ RODZICA MÓWI DRZEWO, nie kolumna. Do 9 września 2026 niosła to kolumna
				// `partOf` w katalogu — działała, ale przynależność była niewidoczna w Studio:
				// grupa stała OBOK swoich pytań jako ich rodzeństwo, a nie nad nimi. Teraz
				// wystarczy rozwinąć gałąź, żeby zobaczyć, o co pyta dany krok.
				question.setPartOf(group.getCode());
				questions.add(question);
			}
			parent.setRequired(anyRequired);
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
		ParamValue row = ruleOf(productElement(), ATTR_SUBJECT_FIELDS, ctx);

		Question question = new Question();
		question.setCode(code);
		question.setSection(BRANCH_SUBJECT);
		question.setText(row.getString("text"));
		question.setKind(row.getString("kind"));
		question.setContextPath(row.getString("contextPath"));
		question.setVisible(Boolean.TRUE.equals(
			ruleOf(productElement(), ATTR_FIELD_VISIBILITY, ctx).getBoolean("visible")));
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
		// ⛔ TREŚĆ PYTANIA WRÓCIŁA DO ATRYBUTU `TEXT` (2026-09-09) — ale to NIE jest powrót do
		// stanu sprzed D-20 i różnica jest cała w tym, czym stała się nazwa. Tamten układ miał
		// TĘ SAMĄ treść w dwóch miejscach: nazwa elementu i atrybut mówiły to samo zdanie, cztery
		// z dziewiętnastu pytań zdążyły się rozjechać, a rozjazd nie objawiał się niczym —
		// konfigurator poprawiał nazwę i ekran się nie zmieniał.
		//
		// Teraz nazwa niesie KRÓTKĄ NAZWĘ PRZEDMIOTOWĄ („Kradzież", „Powódź"), po której czyta się
		// drzewo, a atrybut pełne zdanie zadawane klientowi. Dwie różne rzeczy, więc nie ma czego
		// synchronizować.
		//
		// ⛔ BEZ ZAPASOWEGO ODCZYTU NAZWY. Pytanie dodane bez `TEXT` pokaże na ekranie pustkę —
		// widać to natychmiast i natychmiast się poprawia. Cicha zamiana na nazwę elementu
		// przywróciłaby dokładnie tę niewidoczność, przez którą D-20 usunął atrybut.
		question.setVisible(Boolean.parseBoolean(attrString(definition, ATTR_IS_VISIBLE, ctx)));
		question.setText(attrString(definition, ATTR_TEXT, ctx));
		// ⛔ NOŚNE. `KIND` niesie i rodzaj kontrolki, i typ wartości — `PropertyContext.convert`
		// konwertuje po nim. Do 3 września 2026 były to dwa atrybuty; rozdzielone dawały cztery
		// kombinacje, z których sensowne były dwie, a pomyłka kończyła się tym, że „400" nie
		// stawało się `BigDecimal` i budżet klienta znikał w drodze do rekomendacji.
		// `NUMBER` rozpadł się na `INTEGER` i `DECIMAL` i to wystarczyło, żeby drugi atrybut
		// przestał być potrzebny.
		question.setKind(attrString(definition, ATTR_KIND, ctx));
		question.setContextPath(attrString(definition, ATTR_CONTEXT_PATH, ctx));

		// ⛔ RESZTA TEŻ Z ELEMENTU. Do 10 września 2026 sześć poniższych rzeczy stało w katalogu
		// tabelarycznym, wspólnym dla pytań analizy potrzeb i pól przedmiotu — a katalog robił
		// wtedy dwie różne robotki pod jedną nazwą: pięć jego kolumn było wypełnionych WYŁĄCZNIE
		// dla pól przedmiotu, dwie WYŁĄCZNIE dla pytań w drzewie. To są stałe pojedynczego
		// pytania, więc stoją na pytaniu (D-23); katalog opisuje już tylko pola przedmiotu,
		// które elementami drzewa nie są i nie mają gdzie tego nieść.
		question.setRequired(Boolean.parseBoolean(attrString(definition, ATTR_REQUIRED, ctx)));
		// ⛔ OPCJE WYNIKAJĄ Z RODZAJU ODPOWIEDZI. Pytanie analizy potrzeb jest tak-albo-nie i innego
		// w tym demie nie ma, więc `BOOLEAN` sam mówi, z czego wybierać — atrybut niósłby przy
		// każdym pytaniu tę samą parę wartości. Pola przedmiotu mają własne listy i te siedzą
		// w ich katalogu, bo tam naprawdę różnią się między wierszami.
		boolean yesNo = KIND_BOOLEAN.equals(question.getKind());
		question.setOptions(yesNo ? BOOLEAN_OPTIONS : List.of());
		question.setOptionLabels(yesNo ? BOOLEAN_LABELS : List.of());
		question.setPosition(intOf(attrString(definition, ATTR_POSITION, ctx)));

		return question;
	}


	/** Reszta opisu POLA PRZEDMIOTU — z katalogu, bo pola przedmiotu nie są elementami drzewa. */
	private void applyCatalogue(Question question, ParamValue row) {
		question.setPosition(intOrZero(row.getInteger("position")));
		question.setRequired(Boolean.TRUE.equals(row.getBoolean("required")));
		question.setOptions(split(row.getString("options")));
		question.setOptionLabels(split(row.getString("labels")));
		question.setPrefillSource(row.getString("prefill"));
	}

	/** Liczba z atrybutu tekstowego. Brak i „nie liczba" znaczą tu to samo: koniec listy. */
	private int intOf(String value) {
		if (value == null || value.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}




	/**
	 * Czy pytanie złożone można uznać za udzielone.
	 *
	 * ⚠️ Gdy ŻADNE pole nie jest obowiązkowe, wystarczy jedno udzielone. Warunek „wszystkie"
	 * zamieniłby pytanie z samych pól nieobowiązkowych w ślepy zaułek — dokładnie ten sam błąd,
	 * który 4 września 2026 zrobiła oczekiwana składka po usunięciu odmowy z ekranu.
	 */
	private boolean allRequiredAnswered(PropertyQuote quote, List<Question> fields) {
		boolean anyRequired = false;
		boolean anyAnswered = false;
		for (Question field : fields) {
			// ⛔ Pytanie WARUNKOWE, o które tego klienta nie pytamy, nie liczy się do domknięcia
			// grupy — nie widział go. Bez tego grupa z pytaniem zależnym nigdy by się nie domknęła
			// i ankieta stanęłaby na niej na zawsze.
			if (!field.isVisible()) {
				continue;
			}
			Answer answer = quote.getAnswers().get(field.getCode());
			boolean answered = answer != null && answer.isAnswered();
			anyAnswered = anyAnswered || answered;
			if (field.isRequired()) {
				anyRequired = true;
				if (!answered) {
					return false;
				}
			}
		}
		return anyRequired || anyAnswered;
	}

	/**
	 * Wypełnia nieudzielone odpowiedzi wartościami demonstracyjnymi z tabeli.
	 *
	 * Wartości są w Higsonie, a nie tutaj, z tego samego powodu co reszta: gdyby siedziały w Javie,
	 * kod musiałby znać kody pytań, a nowe pytanie wymagałoby wdrożenia.
	 */
	private void seedDefaults(PropertyQuote quote) {
		for (Question question : quote.getQuestions()) {
			// ⛔ TYLKO PRZEDMIOT UBEZPIECZENIA. Odpowiedź na starcie to dane z poprzedniej polisy,
			// a nie podpowiedziana analiza potrzeb — §18 wyrzuciło je z ankiety właśnie dlatego,
			// że wypełniona z góry analiza potrzeb przeczy temu, co ma pokazywać.
			if (!BRANCH_SUBJECT.equals(question.getSection())) {
				continue;
			}
			Answer answer = quote.getAnswers().get(question.getCode());
			if (answer != null) {
				continue;
			}
			PropertyContext ctx = new PropertyContext(quote).withQuestion(question.getCode());
			String value = ruleOf(productElement(), ATTR_SUBJECT_FIELDS, ctx).getString("demoAnswer");
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

		Map<?, ?> zone = resultOf(productElement(),
			ATTR_ZONES, new PropertyContext(quote));
		quote.getZone().setFlood((String) zone.get("flood"));
		quote.getZone().setTheft((String) zone.get("theft"));

		// ⛔ ZWYKŁA MAPA, nie `ParamValue`. Funkcja Groovy może oddać kilka wartości mapą i to
		// wystarczy — `type.paramValue()` z osobną listą kodów i osobną listą typów opisywał to
		// samo trzy razy dłużej, a jedynym czytelnikiem jest ten kod.
		Map<?, ?> sums = resultOf(productElement(),
			ATTR_SUM_INSURED, new PropertyContext(quote));
		BigDecimal building = asDecimal(sums.get("building"));
		quote.getSums().setSuggestedBuilding(building);
		// „Suma podana ręcznie przez klienta wygrywa z wyliczeniem".
		//
		// ⚠️ Do 9 września 2026 niósł to atrybut produktu `OVERWRITE_MANUAL_SUM = false` (D-23) —
		// przeniesiony tam z tabeli, żeby reguła była widoczna w konfiguracji. Zdjęty na decyzję
		// właściciela: pokrętło, którego nikt nigdy nie przekręcił, zaśmiecało ekran atrybutów.
		// Wraca tu jako warunek. Gdyby kiedyś miało być przestawialne, wraca atrybutem.
		if (!quote.isBuildingSumOverridden()) {
			quote.getSums().setBuilding(building);
		}
		quote.getSums().setFixtures(asDecimal(sums.get("fixtures")));
		quote.getSums().setContents(asDecimal(sums.get("contents")));
		// ⛔ Kradzież, powódź i assistance NIE dostają tu sum. Skąd pokrycie bierze sumę,
		// rozstrzyga atrybut `SUM_BASIS` pokrycia, a wylicza `demo.property.cover.sum`.
		// Do 7 września 2026 stały tu trzy przypisania na sztywno i to one były prawdziwą
		// definicją produktu — tabela o tej samej nazwie istniała, ale nikt jej nie czytał.

		// ⛔ CZYNNIK RYZYKA JEST FLOW, nie funkcją. Siedem tabel czynnikowych i łańcuch mnożeń
		// widać na diagramie zamiast czytać z dwudziestu linii Groovy'ego — a że flow nie umie
		// arytmetyki, mnoży `demo.util.multiply`, dla którego BRAK WARTOŚCI ZNACZY JEDEN.
		Map<?, ?> risk = resultOf(productElement(),
			ATTR_RISK_FACTOR, new PropertyContext(quote));
		quote.setRiskFactor(asDecimal(risk.get("factor")));
	}

	private void loadCovers(PropertyQuote quote) {
		DomainObject product = productElement();
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
			// produktu, a nie mechanika, więc rozstrzyga go dziś WSKAŹNIK `SUM` na pokryciu:
			// powódź pokazuje `sum.building`, kradzież `sum.contents` — czyli wprost mówi, co
			// ubezpiecza, zamiast tłumaczyć to drugim atrybutem.
			//
			// ⚠️ `attrDecimal`, nie `getAttrDecimal`: assistance NIE MA sumy i nie ma tego
			// atrybutu wcale, a pusty atrybut nie powstaje — surowy odczyt leci wtedy NPE.
			cover.setSum(attrDecimal(definition, "SUM", ctx));
			covers.add(cover);
		}
		covers.sort(Comparator.comparingInt(Cover::getPosition));
		quote.setCovers(covers);
	}

	private void priceVariants(PropertyQuote quote) {
		DomainObject product = productElement();
		if (product == null) {
			return;
		}
		List<Variant> variants = new ArrayList<>();
		List<String> priced = new ArrayList<>();

		for (DomainObject definition : product.getChildren(COLLECTION_VARIANTS)) {
			// ⛔ SUMA GWARANCYJNA MUSI BYĆ W KONTEKŚCIE JUŻ TERAZ. Tabela pokrycia OC kluczuje się
			// nią, żeby stawka mogła być degresywna — inaczej każdy pakiet trafia w pierwszy
			// przedział i degresja po cichu nie działa. Suma należy do pakietu, więc znamy ją tutaj.
			//
			// ⚠️ Pakiet BEZ OC nie ma tego atrybutu wcale i to jest właściwy stan: zero znaczyłoby
			// „ochrona na zero złotych", a jej po prostu nie ma. Stąd odczyt odporny na brak.
			PropertyContext ctx = new PropertyContext(quote).withVariant(definition.getCode());
			ctx.withComputed(PATH_SUM_LIABILITY, attrDecimal(definition, ATTR_LIABILITY_SUM, ctx));

			Variant variant = new Variant();
			variant.setCode(definition.getCode());
			variant.setName(definition.getName());
			variant.setDescription(definition.getDescription());
			variant.setPosition(intOrZero(definition.getAttrInteger("POSITION", ctx)));

			applyCovers(definition, variant, ctx);

			Map<?, ?> result = resultOf(definition, ATTR_PREMIUM, ctx);
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
			DomainObject cover = productElement()
				.getChild(COLLECTION_COVERS, member.getCode());
			if (cover == null || !Boolean.TRUE.equals(cover.getAttrBoolean("IS_AVAILABLE", ctx))) {
				continue;
			}
			variant.getCovers().add(member.getCode());
		}
	}

	/**
	 * Analiza potrzeb.
	 *
	 * ⛔ Java chodzi po listach, tabele rozstrzygają. Ani jednego kodu pytania, progu ani zdania
	 * dla klienta nie ma w tym pliku — wszystkie siedzą w `demo.property.apk.*`.
	 */
	private void recommend(PropertyQuote quote) {
		List<Need> needs = collectNeeds(quote);
		quote.setNeeds(needs);

		// Jedna tabela, jeden przebieg, jedna lista. Wyłączenia OWU stykające się z sytuacją
		// klienta (rek. 8.2.c) i ochrona, której klient sam sobie odmówił, choć jego sytuacja
		// tego wymaga (rek. 8.2.b), miały do 3 września 2026 dwie osobne tabele o identycznym
		// kształcie wyjścia. Jedna tabela z dwoma kolumnami stref robi obie rzeczy i czyta się
		// jak lista tego, co trzeba klientowi powiedzieć — bo tym właśnie jest.

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


	/** Produkt — korzeń, od którego zaczyna się każde pytanie tej klasy do Higsona. */
	private DomainObject productElement() {
		return engine.getDomain(PROFILE, PRODUCT_PATH);
	}


	/** Obiekt analizy potrzeb z drzewa — pod jego atrybutami stoi wszystko, o co pytamy ankietę. */
	private DomainObject apkElement() {
		DomainObject product = productElement();
		return product == null ? null : product.getChild(COLLECTION_APK, APK_ELEMENT);
	}

	/**
	 * Reguła stojąca pod atrybutem elementu — CAŁY jej wynik, kolumnę wybiera wołający.
	 *
	 * ⛔ To jest JEDYNA droga tej klasy do silnika, poza odczytem samego drzewa. Wołanie reguły po
	 * kodzie omijało drzewo: adres stał w Javie, więc przestawienie wskaźnika w Studio nie
	 * zmieniało niczego, a skasowanie elementu nie psuło niczego widocznie. Tutaj adresem jest
	 * ELEMENT I ATRYBUT, a to, na co atrybut wskazuje, jest konfiguracją.
	 */
	private ParamValue ruleOf(DomainObject element, String attribute, PropertyContext ctx) {
		return element.getAttribute(attribute).getValue(ctx);
	}

	/**
	 * Reguła, która oddaje KILKA wartości naraz — mapą.
	 *
	 * ⛔ Aplikacja nie wie, czy pod atrybutem stoi funkcja, czy flow, i nie powinna: zamiana
	 * jednego na drugie jest zmianą konfiguracji, a nie kodu. Flow oddaje `FlowResult`, funkcja
	 * zwykłą mapę — a odpowiadają na to samo pytanie.
	 */
	private Map<?, ?> resultOf(DomainObject element, String attribute, PropertyContext ctx) {
		Object raw = ruleOf(element, attribute, ctx).get();
		if (raw instanceof FlowResult flow) {
			return flow.getResult();
		}
		return (Map<?, ?>) raw;
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

		// Kontekst dla reguł rekomendacji: pokrycia, których klient potrzebuje, i składka każdego
		// policzonego pakietu pod własną ścieżką. Reguła nie zna listy potrzeb ani listy pakietów —
		// jedno i drugie czyta z tego, co tu dostanie, i z drzewa.
		//
		// ⛔ Potrzeby są tu FAKTEM, a nie wyprowadzeniem: powstały z reguł (atrybuty pytań plus
		// tabela pól przedmiotu) i są wejściem do doboru pakietu. Liczenie ich drugi raz wewnątrz
		// reguły byłoby tą samą robotą w dwóch miejscach.
		List<String> coverCodes = new ArrayList<>();
		for (Need need : needs) {
			coverCodes.add(need.getCoverCode());
		}
		PropertyContext ctx = new PropertyContext(quote)
			.withComputed(PATH_APK_COVERS, String.join(",", coverCodes));
		for (Variant variant : quote.getVariants()) {
			if (variant.isAvailable() && variant.getAnnualPremium() != null) {
				ctx.withComputed(PATH_PREMIUM_PREFIX + variant.getCode(), variant.getAnnualPremium());
			}
		}

		// ⛔ CAŁY DOBÓR PAKIETU TO JEDEN DIAGRAM — `demo.property.apk.recommend`. Java pyta o niego
		// raz i dostaje trzy odpowiedzi. Rozgałęzienie na diagramie JEST treścią rek. 8.2.d:
		// pakiet wynika z potrzeb, oczekiwana składka tylko go nazywa, a alternatywy szukamy
		// wyłącznie wtedy, gdy propozycja jej nie mieści — obok niej, nigdy zamiast.
		Map<?, ?> decision = resultOf(apkElement(), ATTR_RECOMMENDATION, ctx);

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

		// ⛔ Uzasadnienie powstaje ZAWSZE. Stał tu warunek na przełączniku `requiresJustification`,
		// który tabela ustawiała na `true` i nigdy inaczej — bramka, która nigdy nie zamknęła się
		// ani razu, a wyglądała, jakby coś rozstrzygała. W tym profilu jest jeden reżim (D-17)
		// i jest nim KNF, więc rek. 8.7 obowiązuje bez wyjątku.
		recommendation.setJustification(justification(quote, proposed, score, recommendation));
		return recommendation;
	}

	/**
	 * ETAP 3 — powody decyzji, gotowe zdania.
	 *
	 * ⛔ Jeden odczyt reguły oddaje WARUNEK, TREŚĆ i miejsce w dokumencie — wszystko w jednym
	 * wierszu, więc czytając tabelę widać, kiedy zdanie pada i jak brzmi. Aplikacja podstawia
	 * tylko liczby pod `{0}`, `{1}` i układa zdania po `position`. To nie jest decyzja, tylko
	 * formatowanie.
	 */
	private List<String> justification(PropertyQuote quote, Variant proposed,
			ApkAnalysis.Score score, Recommendation recommendation) {

		DomainObject product = productElement();
		Variant alternative = variantByCode(quote, recommendation.getAlternativeVariantCode());

		List<String> gaps = new ArrayList<>();
		if (alternative != null) {
			for (String cover : ApkAnalysis.score(quote.getNeeds(),
					alternative.getCovers()::contains).getMissing()) {
				gaps.add(nameOf(product, COLLECTION_COVERS, cover));
			}
		}
		List<String> missingNames = new ArrayList<>();
		for (String cover : score.getMissing()) {
			missingNames.add(nameOf(product, COLLECTION_COVERS, cover));
		}

		BigDecimal budget = decimalAt(quote, PATH_BUDGET);
		BigDecimal declared = decimalAt(quote, PATH_PROPERTY_VALUE);
		BigDecimal computed = decimalAt(quote, PATH_SUM_SUGGESTED);
		ParamValue flood = ruleOf(product.getChild(COLLECTION_COVERS, COVER_FLOOD),
			ATTR_DEDUCTIBLE, new PropertyContext(quote));
		quote.setFloodDeductible(flood.getBigDecimal("minDeductible"));
		ParamValue waiting = ruleOf(product.getChild(COLLECTION_COVERS, COVER_FLOOD),
			ATTR_WAITING_PERIOD, new PropertyContext(quote));

		List<String[]> ordered = new ArrayList<>();
		for (MultiValue row : justificationRows(quote, proposed, alternative)) {
			Object[] arguments = switch (row.getString("reasonCode")) {
				case "NEEDS_MATCHED", "NEEDS_PARTIAL" -> new Object[] {
					nameOf(product, COLLECTION_VARIANTS, proposed.getCode()),
					score.getCovered(), score.getTotal(), String.join(", ", missingNames) };
				case "SUM_COMPUTED" -> new Object[] {
					new PropertyContext(quote).get(PATH_AREA),
					ruleOf(product, ATTR_SUM_RATE, new PropertyContext(quote)).getBigDecimal("rate") };
				case "VALUE_UNDERSTATED", "VALUE_CONSISTENT", "VALUE_OVERSTATED" ->
					new Object[] { money(declared), money(computed) };
				case "BUDGET_FIT", "BUDGET_EXCEEDED" ->
					new Object[] { money(proposed.getAnnualPremium()), money(budget) };
				case "BUDGET_NO_ALTERNATIVE" -> new Object[] { money(budget) };
				case "BUDGET_ALTERNATIVE_EQUAL" -> new Object[] {
					nameOf(product, COLLECTION_VARIANTS, alternative.getCode()),
					money(alternative.getAnnualPremium()) };
				case "BUDGET_ALTERNATIVE" -> new Object[] {
					nameOf(product, COLLECTION_VARIANTS, alternative.getCode()),
					money(alternative.getAnnualPremium()), String.join(", ", gaps) };
				case "FLOOD_DEDUCTIBLE" -> new Object[] { money(flood.getBigDecimal("minDeductible")) };
				case "FLOOD_WAITING" -> new Object[] { waiting.getInteger("days") };
				default -> new Object[0];
			};
			String text = row.getString("text");
			for (int i = 0; i < arguments.length; i++) {
				text = text.replace("{" + i + "}", String.valueOf(arguments[i]));
			}
			ordered.add(new String[] { String.format("%04d", intOrZero(row.getInteger("position"))), text });
		}

		ordered.sort(Comparator.comparing(entry -> entry[0]));
		List<String> lines = new ArrayList<>();
		for (String[] entry : ordered) {
			lines.add(entry[1]);
		}
		return lines;
	}


	/**
	 * ETAP 3 — powody decyzji. Wszystkie naraz, bo poziomy tabeli mają `union`.
	 *
	 * ⛔ Do kontekstu idą DANE, nie wnioski: potrzeby klienta, kod i cena proponowanego pakietu,
	 * kod alternatywy. Czy pakiet pokrywa wszystko, czy suma jest niższa od wyliczonej i czy cena
	 * mieści się w oczekiwanej składce — stwierdzają funkcje podpięte pod `source` kolumn, więc
	 * porównania są tam, gdzie widać, co z czym jest porównywane.
	 */
	private List<MultiValue> justificationRows(PropertyQuote quote, Variant proposed, Variant alternative) {
		List<String> coverCodes = new ArrayList<>();
		for (Need need : quote.getNeeds()) {
			coverCodes.add(need.getCoverCode());
		}
		PropertyContext ctx = new PropertyContext(quote)
			.withComputed(PATH_APK_COVERS, String.join(",", coverCodes))
			.withComputed(PATH_APK_PROPOSED, proposed.getCode())
			.withComputed(PATH_APK_PROPOSED_PREMIUM, proposed.getAnnualPremium())
			.withComputed(PATH_APK_ALTERNATIVE, alternative == null ? null : alternative.getCode());

		List<MultiValue> rows = new ArrayList<>();
		for (MultiValue row : ruleOf(apkElement(), ATTR_JUSTIFICATION, ctx)) {
			if (row.getString("reasonCode") != null) {
				rows.add(row);
			}
		}
		return rows;
	}

	/**
	 * Warunki oferty: ważność, termin płatności, treść oświadczenia i to, czy do oferty dołącza
	 * się dokument analizy potrzeb.
	 *
	 * ⛔ Wszystkie cztery to ATRYBUTY PRODUKTU, nie tabela (D-23). Do 9 września 2026 niosła je
	 * `demo.property.offer.terms` kluczowana kodem reżimu — konstrukcja sensowna, dopóki reżimy
	 * były dwa i przesunięcie daty polisy przez próg KNF przestawiało treść oferty. D-17 usunął
	 * region i drugi reżim, więc została tabela z jednym żywym wierszem i wierszem gwiazdkowym,
	 * do którego nic nie trafiało: zero drugiego wymiaru, czyli dokładnie ten kształt, który
	 * D-23 każe zapisać na elemencie.
	 *
	 * ⚠️ Razem z nią zniknęła `demo.property.apk.regime`. Jej `code` był kluczem WYŁĄCZNIE do
	 * tamtej tabeli, a `name` nie jechało nawet na drut, odkąd D-17 zabrał ekranowi panel
	 * ustawień. Gdyby reżimy kiedyś wróciły, wracają obie razem — to jest jedna konstrukcja.
	 *
	 * ⚠️ Numer i daty przelicza się przy każdej wycenie, ale TYLKO dopóki polisa nie jest
	 * wystawiona: po wystawieniu dokument jest już wydany i zmiana odpowiedzi nie ma prawa
	 * przestawić mu numeru ani okresu ochrony.
	 */
	/**
	 * Czy pakiet o tym kodzie w ogóle istnieje w konfiguracji.
	 *
	 * ⛔ Pytanie do DRZEWA, nie do policzonej wyceny. Sprawdzanie po `quote.getVariants()` wygląda
	 * naturalnie i jest błędne: zaraz po `PUT /reset` ta lista jest pusta, bo wypełnia ją dopiero
	 * `calculate()` przy najbliższym odczycie — więc poprawny kod dostawał odmowę tylko dlatego,
	 * że nikt jeszcze nie zdążył zapytać o wycenę.
	 */
	public boolean hasVariant(String code) {
		DomainObject product = productElement();
		return product != null && product.getChild(COLLECTION_VARIANTS, code) != null;
	}

	private void prepareOffer(PropertyQuote quote) {
		Offer offer = quote.getOffer();
		// Wystawiony dokument ma numer i datę — zmiana odpowiedzi nie ma prawa przestawić mu
		// okresu ani numeru.
		//
		// ⚠️ Do 9 września 2026 niósł to atrybut produktu `FREEZE_ISSUED_OFFER = true` (D-23),
		// zdjęty razem z `OVERWRITE_MANUAL_SUM` z tego samego powodu — patrz `computeDerived`.
		if (offer.isIssued()) {
			return;
		}

		LocalDate today = LocalDate.now();

		offer.setNumber(offer.getNumber() == null ? numbers.nextOffer(today) : offer.getNumber());
		offer.setIssuedOn(today);
		offer.setValidUntil(today.plusDays(productInt(quote, ATTR_OFFER_VALIDITY_DAYS)));
		offer.setPaymentDueOn(today.plusDays(productInt(quote, ATTR_OFFER_PAYMENT_DAYS)));
		offer.setStatement(productText(quote, ATTR_OFFER_STATEMENT));
		offer.setNeedsAnalysisAttached(productFlag(quote, ATTR_OFFER_NEEDS_ANALYSIS));

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
		//
		// ⛔ JEDEN ODCZYT, CAŁY WIERSZ. Do 11 września 2026 czytały się tu dwie reguły: pierwsza
		// nazywała rodzaj wyboru, druga po tym kodzie dobierała zdania. Każdy z trzech rodzajów
		// przechodził przez obie, więc kod istniał wyłącznie po to, żeby przejść do drugiej
		// tabeli — a Java odkładała go do kontekstu, żeby sama sobie go potem podać.
		DomainObject product = productElement();
		ParamValue note = ruleOf(product, ATTR_CHOICE_RELATION,
			new PropertyContext(quote).withChoiceVariants(recommended, chosen));
		String kind = note.getString("kind");
		if (kind == null) {
			offer.setChoiceKind(null);
			return;
		}

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
			DomainObject product = productElement();
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
		return intOrZero(ruleOf(productElement(),
			ATTR_COVER_PERIOD, new PropertyContext(quote)).getInteger("coverMonths"));
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


	/**
	 * Lista potrzeb z reguły: `POKRYCIE|treść|waga`, rekordy rozdzielone średnikiem.
	 *
	 * ⚠️ Rozdzielamy po PIERWSZYCH dwóch kreskach, nie przez `split` po całości — treść potrzeby
	 * jest zdaniem dla klienta i nie ma powodu, żeby kod aplikacji dyktował, czego w niej nie ma.
	 */

	/**
	 * ETAP 1 — potrzeby klienta. DWIE TABELE, bo są dwa źródła potrzeby.
	 *
	 * ⛔ Aplikacja nie chodzi po pytaniach i niczego nie skleja. Odpowiedzi są już w kontekście
	 * pod swoimi ścieżkami, a każda z tabel ma jako kolumny TE ODPOWIEDZI, z których czyta:
	 * `…need.declared` pyta o ankietę, `…need.inferred` o przedmiot ubezpieczenia. Poziomy mają
	 * `union`, więc jeden odczyt oddaje wszystkie pasujące wiersze naraz.
	 *
	 * ⛔ ŹRÓDŁO POTRZEBY TO TABELA, W KTÓREJ STOI — nie kolumna w niej. Poprzednia wersja miała
	 * jedną tabelę o dziesięciu kolumnach wejściowych i kolumnie `priority`, w której 1 znaczyło
	 * „zgłoszona", a 2 „wywnioskowana"; siedem kolumn każdego wiersza było gwiazdkami, bo połowa
	 * wierszy nie miała nic wspólnego z drugą połową. Rozdzielone, każda tabela ma tylko własne
	 * pytania, a to, co niosła kolumna, niesie przynależność.
	 *
	 * ⛔ PIERWSZEŃSTWO WYNIKA Z KOLEJNOŚCI ODCZYTU: zgłoszone najpierw, więc gdy dwie odpowiedzi
	 * wskazują to samo pokrycie — „obawiam się kradzieży" i „nie mam zabezpieczeń" obie dają
	 * THEFT — klient słyszy zdanie o tym, co powiedział sam. Rek. 8.3 wymaga, żeby wiedział,
	 * które potrzeby wywnioskowaliśmy za niego; to niesie `declared`.
	 */
	private List<Need> collectNeeds(PropertyQuote quote) {
		Map<String, Need> byCover = new LinkedHashMap<>();
		collectNeedsFrom(quote, ATTR_NEEDS_DECLARED, true, byCover);
		collectNeedsFrom(quote, ATTR_NEEDS_INFERRED, false, byCover);
		return new ArrayList<>(byCover.values());
	}

	/** Jedna potrzeba na pokrycie — wygrywa tabela odczytana wcześniej. */
	private void collectNeedsFrom(PropertyQuote quote, String attribute, boolean declared,
			Map<String, Need> byCover) {

		for (MultiValue row : ruleOf(apkElement(), attribute, new PropertyContext(quote))) {
			String cover = row.getString("cover");
			if (cover != null && !cover.isEmpty() && !byCover.containsKey(cover)) {
				Need need = new Need(cover, row.getString("need"), 0);
				need.setDeclared(declared);
				byCover.put(cover, need);
			}
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

	/** Liczba z atrybutu, którego może nie być — pusty atrybut nie powstaje, patrz `attrString`. */
	private BigDecimal attrDecimal(DomainObject definition, String code, PropertyContext ctx) {
		return definition.getAttribute(code) == null ? null : definition.getAttrDecimal(code, ctx);
	}

	/**
	 * Przełącznik wpisany wprost w atrybut produktu (D-23).
	 *
	 * ⛔ To NIE jest powrót logiki do Javy. Wartość dalej rozstrzyga konfiguracja — tyle że stoi
	 * na elemencie, którego dotyczy, zamiast w tabeli o jednym wierszu i jednym wejściu, które
	 * nigdy się nie zmieniało. Tabela zostaje wtedy, gdy ma drugi wymiar.
	 */
	private boolean productFlag(PropertyQuote quote, String code) {
		return Boolean.parseBoolean(productText(quote, code));
	}

	/**
	 * Stała produktu jako tekst — patrz D-23.
	 *
	 * ⚠️ Null-safe przez `attrString`: pusty atrybut LITERAL po prostu NIE ISTNIEJE w modelu,
	 * a `getAttrString` na nieistniejącym rzuca NPE zamiast oddać null.
	 */
	private String productText(PropertyQuote quote, String code) {
		DomainObject product = productElement();
		return product == null ? null : attrString(product, code, new PropertyContext(quote));
	}

	/** Stała produktu jako liczba całkowita. Brak i „nie liczba" znaczą tu to samo: zero. */
	private int productInt(PropertyQuote quote, String code) {
		String value = productText(quote, code);
		if (value == null || value.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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
