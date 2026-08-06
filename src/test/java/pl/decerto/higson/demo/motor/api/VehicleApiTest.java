package pl.decerto.higson.demo.motor.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * P-3 contract goldens for {@code VehicleApi} - endpoints 16-20 of the frozen 21-endpoint REST
 * contract (spec 6.2).
 *
 * <p><b>Requires a populated Higson snapshot (P-2); a fresh clone cannot run this class</b> until
 * {@code mvn higson-cli:pull} has filled {@code src/main/higson}. See {@link ApiContractTestSupport}.
 */
class VehicleApiTest extends ApiContractTestSupport {

	/**
	 * Endpoint 16. All five fields are primitive {@code int} or {@code String} server-side, so the
	 * numeric ones are <b>never null - unset is 0</b>. That is the property the Angular vehicle
	 * cascade's falsy-zero guards depend on, which is why the target model types them {@code number}
	 * and never {@code number | null}.
	 */
	@Test
	void vehicleIsReturnedWithPrimitiveIntFieldsDefaultingToZero() throws Exception {
		MockHttpSession session = newSession();

		JsonNode vehicle = getJson("/vehicle", session);
		assertEquals(2010, vehicle.get("productionYear").asInt(), "default productionYear");
		assertEquals(217, vehicle.get("makeId").asInt(), "default makeId");
		assertEquals("TOYOTA", vehicle.get("make").asText(), "default make");
		assertEquals(28654, vehicle.get("typeId").asInt(), "default typeId");
		assertEquals(218915, vehicle.get("modelId").asInt(), "default modelId");
		assertEquals(5, vehicle.size(), "GET vehicle must expose exactly 5 fields");

		assertEquals(200, putJson("/vehicle/productionYear", "2015", session).getStatus(),
				"PUT vehicle/productionYear takes a JSON number");

		JsonNode cleared = getJson("/vehicle", session);
		for (String id : new String[] {"makeId", "typeId", "modelId"}) {
			assertTrue(cleared.get(id).isInt(), id + " must stay a JSON number after being cleared");
			assertEquals(0, cleared.get(id).asInt(), id + " must read back as 0, never null, once cleared");
		}
	}

	/**
	 * Endpoints 17-20 including the <b>server-side cascade side effects</b> the Angular client must
	 * not duplicate: the backend already clears the dependent identifiers on every level change.
	 * Production year is exercised last because it clears all three.
	 */
	@Test
	void updatingAMakeTypeOrProductionYearClearsTheDependentServerSideIds() throws Exception {
		MockHttpSession session = newSession();

		assertEquals(200, putJson("/vehicle/make", "{\"makeId\":220,\"make\":\"VOLKSWAGEN\"}", session).getStatus(),
				"PUT vehicle/make takes a JSON object");
		JsonNode afterMake = getJson("/vehicle", session);
		assertEquals(220, afterMake.get("makeId").asInt(), "make change stores the new makeId");
		assertEquals("VOLKSWAGEN", afterMake.get("make").asText(), "make change stores the new make label");
		assertEquals(0, afterMake.get("typeId").asInt(), "make change clears typeId server-side");
		assertEquals(0, afterMake.get("modelId").asInt(), "make change clears modelId server-side");
		assertEquals(2010, afterMake.get("productionYear").asInt(), "make change leaves productionYear alone");

		assertEquals(200, putJson("/vehicle/type", "28654", session).getStatus(), "PUT vehicle/type takes a JSON number");
		JsonNode afterType = getJson("/vehicle", session);
		assertEquals(28654, afterType.get("typeId").asInt(), "type change stores the new typeId");
		assertEquals(0, afterType.get("modelId").asInt(), "type change clears modelId server-side");
		assertEquals(220, afterType.get("makeId").asInt(), "type change leaves makeId alone");

		assertEquals(200, putJson("/vehicle/model", "218915", session).getStatus(), "PUT vehicle/model takes a JSON number");
		JsonNode afterModel = getJson("/vehicle", session);
		assertEquals(218915, afterModel.get("modelId").asInt(), "model change stores the new modelId");
		assertEquals(28654, afterModel.get("typeId").asInt(), "model change clears nothing - it is the leaf of the cascade");
		assertEquals(220, afterModel.get("makeId").asInt(), "model change leaves makeId alone");

		assertEquals(200, putJson("/vehicle/productionYear", "2020", session).getStatus(),
				"PUT vehicle/productionYear takes a JSON number");
		JsonNode afterYear = getJson("/vehicle", session);
		assertEquals(2020, afterYear.get("productionYear").asInt(), "production year change stores the new year");
		assertEquals(0, afterYear.get("makeId").asInt(), "production year change clears makeId server-side");
		assertEquals(0, afterYear.get("typeId").asInt(), "production year change clears typeId server-side");
		assertEquals(0, afterYear.get("modelId").asInt(), "production year change clears modelId server-side");
	}
}
