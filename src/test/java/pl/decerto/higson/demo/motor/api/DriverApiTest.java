package pl.decerto.higson.demo.motor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * P-3 contract goldens for {@code DriverApi} - endpoints 5-9 and 13-15 of the frozen 21-endpoint
 * REST contract (spec 6.2).
 *
 * <p><b>Requires a populated Higson snapshot (P-2); a fresh clone cannot run this class</b> until
 * {@code mvn higson-cli:pull} has filled {@code src/main/higson}. See {@link ApiContractTestSupport}.
 */
class DriverApiTest extends ApiContractTestSupport {

	/**
	 * Endpoint 5. The default driver seeded by {@code Motor.createDriver}, and in particular
	 * {@code dateOfBirth} as an <b>ISO-8601 string carrying its {@code +00:00} offset</b> - not epoch
	 * millis. Phase 1 stated the opposite; PC-3 corrected it empirically, and typing the Angular model
	 * {@code number} would break the {@code MatDatepicker} binding.
	 */
	@Test
	void driverIsReturnedWithIsoEightSixOhOneBirthDateAndOffset() throws Exception {
		JsonNode driver = getJson("/driver", newSession());

		String expectedBirthDate = LocalDate.now(ZoneOffset.UTC).minusYears(40) + "T00:00:00.000+00:00";
		assertEquals(expectedBirthDate, driver.get("dateOfBirth").asText(),
				"dateOfBirth must be an ISO-8601 string with a +00:00 offset (P-4 pins the JVM to UTC)");
		assertTrue(driver.get("dateOfBirth").isTextual(),
				"dateOfBirth must serialize as a JSON string, never as epoch millis");

		assertEquals("John", driver.get("firstName").asText(), "default firstName");
		assertEquals("Potter", driver.get("lastName").asText(), "default lastName");
		assertEquals("M", driver.get("gender").asText(), "default gender");
		assertEquals(0, driver.get("numberOfAccidents").asInt(), "numberOfAccidents defaults to 0, never null");
		assertEquals(0, driver.get("numberOfTickets").asInt(), "numberOfTickets defaults to 0, never null");
		assertEquals(18, driver.get("licenceObtainedAtAge").asInt(), "default licenceObtainedAtAge");

		JsonNode address = driver.get("address");
		assertEquals("Lake Jackson", address.get("city").asText(), "default address.city");
		assertEquals("Allwood St", address.get("street").asText(), "default address.street");
		assertEquals("77566", address.get("zipCode").asText(), "default address.zipCode");

		assertEquals(8, driver.size(), "GET driver must expose exactly 8 top-level fields");
		assertEquals(3, address.size(), "address must expose exactly zipCode, city and street");
	}

	/**
	 * Endpoints 7, 8, 9, 13, 14 and 15. Every string PUT is sent as a <b>raw, unquoted body with
	 * {@code Content-Type: text/plain}</b> - exactly what Angular's {@code HttpClient} emits for a
	 * {@code string} body - and read back through {@code GET driver} to prove it is stored verbatim.
	 *
	 * <p>The trailing control case is the point of this test: a body that <i>does</i> carry quote
	 * characters is stored <i>with</i> them. A stray {@code JSON.stringify} in the Angular service
	 * layer would silently corrupt all six fields exactly this way.
	 */
	@Test
	void stringFieldsAcceptRawUnquotedTextPlainBodiesAndReadBackUncorrupted() throws Exception {
		MockHttpSession session = newSession();

		assertEquals(200, putText("/driver/firstName", "Anna", session).getStatus(), "PUT driver/firstName as text/plain");
		assertEquals(200, putText("/driver/gender", "F", session).getStatus(), "PUT driver/gender as text/plain");
		assertEquals(200, putText("/driver/lastName", "Kowalska", session).getStatus(), "PUT driver/lastName as text/plain");
		assertEquals(200, putText("/driver/address/zipCode", "31-001", session).getStatus(), "PUT driver/address/zipCode as text/plain");
		assertEquals(200, putText("/driver/address/city", "Krakow", session).getStatus(), "PUT driver/address/city as text/plain");
		assertEquals(200, putText("/driver/address/street", "Florianska", session).getStatus(), "PUT driver/address/street as text/plain");

		JsonNode driver = getJson("/driver", session);
		assertEquals("Anna", driver.get("firstName").asText(), "firstName must be stored verbatim");
		assertEquals("F", driver.get("gender").asText(), "gender must be stored verbatim");
		assertEquals("Kowalska", driver.get("lastName").asText(), "lastName must be stored verbatim");
		assertEquals("31-001", driver.get("address").get("zipCode").asText(), "zipCode must be stored verbatim");
		assertEquals("Krakow", driver.get("address").get("city").asText(), "city must be stored verbatim");
		assertEquals("Florianska", driver.get("address").get("street").asText(), "street must be stored verbatim");

		assertEquals(200, putText("/driver/address/city", "\"Krakow\"", session).getStatus(),
				"the quoted control body must still be accepted");
		assertEquals("\"Krakow\"", getJson("/driver", session).get("address").get("city").asText(),
				"a body carrying quote characters is stored with them - this is how a stray JSON.stringify corrupts a string PUT");
	}

	/**
	 * Endpoint 6, and the one endpoint whose encoding rule is the <b>opposite</b> of its six string
	 * neighbours. {@code DriverApi.updateBirthDate} takes {@code @RequestBody Date} with
	 * {@code required} unset, so a body is mandatory and Jackson must be given
	 * {@code application/json}. The same ISO value sent as {@code text/plain} - which is what Angular
	 * would emit if the caller passed a {@code string} instead of a {@code Date} - is rejected with
	 * <b>415</b>. Proves AC-C3.
	 */
	@Test
	void birthDateRequiresJsonQuotedIsoStringAndRejectsTextPlain() throws Exception {
		MockHttpSession session = newSession();

		MockHttpServletResponse accepted = putJson("/driver/birthDate", "\"1990-05-12T00:00:00.000Z\"", session);
		assertEquals(200, accepted.getStatus(), "a JSON-quoted ISO string is the accepted birthDate encoding");
		assertEquals("1990-05-12T00:00:00.000+00:00", getJson("/driver", session).get("dateOfBirth").asText(),
				"the stored birth date reads back as an ISO-8601 string with a +00:00 offset");

		MockHttpServletResponse rejected = putText("/driver/birthDate", "1990-05-12T00:00:00.000Z", session);
		assertEquals(415, rejected.getStatus(),
				"the same value as text/plain must be rejected with 415 - Jackson cannot bind Date from text/plain");
	}
}
