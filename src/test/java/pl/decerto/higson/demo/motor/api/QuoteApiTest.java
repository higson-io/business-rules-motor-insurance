package pl.decerto.higson.demo.motor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * P-3 contract goldens for {@code QuoteApi} - endpoint 21 of the frozen 21-endpoint REST contract
 * (spec 6.2), plus the error contract that {@code GET quote} produces today.
 *
 * <p><b>Requires a populated Higson snapshot (P-2); a fresh clone cannot run this class</b> until
 * {@code mvn higson-cli:pull} has filled {@code src/main/higson}. Every {@code GET quote} runs a full
 * rules evaluation, so this class exercises the engine end to end. See {@link ApiContractTestSupport}.
 */
class QuoteApiTest extends ApiContractTestSupport {

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
	 * P-3 golden of the pre-C-6 contract. G10/DR-14 replaces this method with
	 * {@code quoteReturnsFourHundredWithValidationMessage()}. Deleting or editing it outside G10 is a
	 * regression.
	 *
	 * <p>Today a failed {@code GET quote} does not surface the {@code QuoteValidator} message at all:
	 * {@code GlobalExceptionHandler} is a plain {@code @ControllerAdvice} whose handlers return a
	 * {@code String} without {@code @ResponseBody}, so Spring MVC treats the message as a <b>view
	 * name</b> and dispatches a forward to it. The response body therefore never carries a
	 * {@code message} key, which is why the AngularJS error banner renders with no text at all.
	 *
	 * <p>⚠️ <b>MockMvc sees only part of this defect.</b> In a real servlet container the forward
	 * lands on a path with no handler, and <i>that</i> 404 overrides the
	 * {@code @ResponseStatus(BAD_REQUEST)} - which is the {@code 404} with body
	 * {@code {"timestamp":…,"status":404,"error":"Not Found","path":"…"}} recorded in spec 6.4 and
	 * re-verified live while capturing these goldens. MockMvc's {@code MockRequestDispatcher} only
	 * <i>records</i> a forward instead of executing it, so the status stays at 400 here. The status
	 * half of DR-14 is therefore pinned by the Playwright capture (G2, {@code error-paths.spec.ts});
	 * what this method pins is the half MockMvc can see, and it is the half that flips in G10: the
	 * validator message leaves as a view name and never as a response body.
	 */
	@Test
	void quoteFailureForwardsTheValidatorMessageAsAViewNameAndCarriesNoMessageKey() throws Exception {
		MockHttpSession session = newSession();
		assertEquals(200, putText("/driver/firstName", "", session).getStatus(),
				"blanking the first name is itself accepted - only the quote recalculation fails");

		MockHttpServletResponse response = get("/quote", session);
		assertEquals("Driver first name not supplied.", response.getForwardedUrl(),
				"the validator message is dispatched as a view name, which is the whole defect C-6 fixes");
		assertEquals(400, response.getStatus(),
				"under MockMvc the @ResponseStatus survives; a real container's failed forward turns this into 404 (spec 6.4)");
		assertFalse(response.getContentAsString().contains("\"message\""),
				"today's error body carries no message key, so the client banner has nothing to render");
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
