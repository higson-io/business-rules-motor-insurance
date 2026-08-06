package pl.decerto.higson.demo.motor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * P-3 contract goldens for {@code DictionaryApi} - endpoints 1-4 of the frozen 21-endpoint REST
 * contract (spec 6.2).
 *
 * <p><b>Requires a populated Higson snapshot (P-2); a fresh clone cannot run this class</b> until
 * {@code mvn higson-cli:pull} has filled {@code src/main/higson}. The dictionary rows below come
 * straight out of the snapshot, so this class doubles as evidence that the rules are loaded. See
 * {@link ApiContractTestSupport}.
 */
class DictionaryApiTest extends ApiContractTestSupport {

	/**
	 * Endpoints 1-4. Every entry is {@code {code, name}} and <b>{@code code} is a String</b> even
	 * where it carries a numeric identifier - which is why the Angular binding sites must coerce it
	 * with an explicit {@code Number()} before comparing it to the {@code number}-typed vehicle ids.
	 */
	@Test
	void dictionaryEntriesExposeCodeAsAString() throws Exception {
		MockHttpSession session = newSession();

		assertEntriesExposeCodeAsAString(getJson("/dictionaries/productionYear", session), "productionYear");

		JsonNode makes = getJson("/dictionaries/make?productionYear=2010", session);
		assertEntriesExposeCodeAsAString(makes, "make");
		assertTrue(containsEntry(makes, "217", "TOYOTA"),
				"the make dictionary for 2010 must contain the seeded default vehicle's make as code \"217\"");

		assertEntriesExposeCodeAsAString(getJson("/dictionaries/type?makeId=217", session), "type");
		assertEntriesExposeCodeAsAString(getJson("/dictionaries/model?typeId=28654", session), "model");
	}

	private static boolean containsEntry(JsonNode entries, String code, String name) {
		for (JsonNode entry : entries) {
			if (code.equals(entry.get("code").asText()) && name.equals(entry.get("name").asText())) {
				return true;
			}
		}
		return false;
	}

	private static void assertEntriesExposeCodeAsAString(JsonNode entries, String dictionary) {
		assertTrue(entries.isArray(), dictionary + " dictionary must be a JSON array");
		assertTrue(entries.size() > 0, dictionary + " dictionary must not be empty - the snapshot supplies its rows");

		for (JsonNode entry : entries) {
			assertEquals(2, entry.size(), dictionary + " entry must expose exactly code and name");
			assertTrue(entry.get("code").isTextual(),
					dictionary + " entry code must be a JSON string, never a number: " + entry);
			assertTrue(entry.get("name").isTextual(),
					dictionary + " entry name must be a JSON string: " + entry);
		}
	}
}
