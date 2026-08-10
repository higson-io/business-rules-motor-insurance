package pl.decerto.higson.demo.motor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * P-3 contract goldens for {@code QuoteApi} - endpoint 21 of the frozen 21-endpoint REST contract
 * (spec 6.2), plus the error contract that {@code GET quote} produces today.
 *
 * <p><b>Requires a populated Higson snapshot (P-2); a fresh clone cannot run this class</b> until
 * {@code mvn higson-cli:pull} has filled {@code src/main/higson}. Every {@code GET quote} runs a full
 * rules evaluation, so this class exercises the engine end to end. See {@link ApiContractTestSupport}.
 */
class QuoteApiTest extends ApiContractTestSupport {

	/** Only the error body is parsed here; the success bodies come back through {@code getJson}. */
	private static final ObjectMapper JSON = new ObjectMapper();

	/**
	 * Endpoint 21 and the <b>DR-05 evidence</b>: {@code Motor.calculate} already sorts coverages and
	 * discounts by {@code position} before the converter runs, so the Angular quote panel must add
	 * <b>no</b> client-side sort. The AngularJS template's {@code orderBy} filter is therefore
	 * redundant and is removed without a replacement.
	 */
	@Test
	void quoteReturnsCoveragesAndDiscountsAlreadySortedByPosition() throws Exception {
		JsonNode quote = getJson("/quote", newSession());

		assertEquals(1, quote.size(), "QuoteDto must expose exactly one field: options");
		JsonNode options = quote.get("options");
		assertEquals(3, options.size(), "the FULL plan must yield three rating options");

		for (JsonNode option : options) {
			String code = option.get("code").asText();
			assertEquals(6, option.size(),
					"option " + code + " must expose exactly code, coverages, discounts, order, premium and premiumBeforeDiscounts");

			JsonNode coverages = option.get("coverages");
			assertTrue(coverages.size() > 0, "option " + code + " must carry at least one coverage");
			assertAscendingPositions(coverages, "coverages of option " + code);
			for (JsonNode coverage : coverages) {
				assertEquals(7, coverage.size(), "coverage " + coverage.get("code").asText()
						+ " of option " + code + " must expose exactly position, code, name, limitLeft, limitRight, premium and description");
			}

			assertAscendingPositions(option.get("discounts"), "discounts of option " + code);
		}
	}

	/**
	 * The <b>deliberate golden flip</b>, authorised by <b>DR-14 / C-6</b> (spec 6.4) and sequenced as
	 * task group 10 so the contract change is made by the register rather than discovered as a
	 * failure during the G11 replay. This method <b>replaces</b> the P-3 golden
	 * {@code quoteFailureForwardsTheValidatorMessageAsAViewNameAndCarriesNoMessageKey()}, which
	 * recorded the pre-fix contract; that method's own Javadoc named G10/DR-14 as the only sanctioned
	 * authority for removing it.
	 *
	 * <p><b>What changed.</b> {@code GlobalExceptionHandler} was a plain {@code @ControllerAdvice}
	 * whose handlers returned a {@code String} without {@code @ResponseBody}, so Spring MVC treated
	 * the validator message as a <b>view name</b> and dispatched a forward to it. In a real container
	 * that forward lands on a path with no handler and the resulting <b>404 overrides</b> the
	 * {@code @ResponseStatus(BAD_REQUEST)} - verified live before the fix as
	 * {@code 404 {"timestamp":…,"status":404,"error":"Not Found","path":"/demo/quote"}}, with no
	 * {@code message} key at all. It is now a {@code @RestControllerAdvice} returning
	 * {@code ErrorDto}, so the response is serialised as the body and the {@code @ResponseStatus}
	 * takes effect.
	 *
	 * <p>⚠️ <b>The status alone does not prove the fix under MockMvc.</b>
	 * {@code MockRequestDispatcher} only <i>records</i> a forward instead of executing it, so the
	 * status read 400 here even before the fix (recorded by G1). The load-bearing assertions are
	 * therefore the <b>body carrying {@code message}</b> and {@code forwardedUrl} being <b>null</b> -
	 * the latter is the direct evidence that the view-name dispatch is gone. The 404→400 half of
	 * DR-14 is pinned live by the Playwright capture ({@code e2e/specs/error-paths.spec.ts}).
	 */
	@Test
	void quoteReturnsFourHundredWithValidationMessage() throws Exception {
		MockHttpSession session = newSession();
		assertEquals(200, putText("/driver/firstName", "", session).getStatus(),
				"blanking the first name is itself accepted - only the quote recalculation fails");

		MockHttpServletResponse response = get("/quote", session);
		assertEquals(400, response.getStatus(),
				"DR-14: a rejected quote answers 400, and no failed view resolution turns it into 404 any more");
		assertNull(response.getForwardedUrl(),
				"DR-14: the validator message is a response body now, not a view name - no forward is dispatched");

		JsonNode body = JSON.readTree(response.getContentAsString());
		assertEquals(1, body.size(),
				"ErrorDto must expose exactly one field: message");
		assertEquals("Driver first name not supplied.", body.get("message").asText(),
				"the banner renders this verbatim, so the QuoteValidator message is the contract");
	}

	private static void assertAscendingPositions(JsonNode elements, String description) {
		List<Integer> positions = new ArrayList<>();
		for (JsonNode element : elements) {
			positions.add(element.get("position").asInt());
		}
		for (int i = 1; i < positions.size(); i++) {
			assertTrue(positions.get(i - 1) <= positions.get(i),
					"the backend already returns " + description + " sorted by position ascending, but got " + positions);
		}
	}
}
