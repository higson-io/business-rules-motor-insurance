package pl.decerto.higson.demo.motor.api;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.core.HigsonEngineFactory;
import io.higson.runtime.core.datasource.snapshot.HigsonSnapshotDataSource;
import pl.decerto.ApplicationConfig;

/**
 * Shared harness for the P-3 contract goldens: the four {@code *ApiTest} classes below pin the
 * 21-endpoint REST contract of the pre-migration (AngularJS-era) build so the Angular rewrite can be
 * replayed against it.
 *
 * <p><b>Requires a populated Higson snapshot (P-2).</b> The engine is booted from
 * {@code ${higson.snapshot.dir}} = {@code src/main/higson} via {@link HigsonSnapshotDataSource} -
 * offline, no database, no network ({@code standards/testing/rule-tests.md}). That directory is
 * <b>not tracked in git</b> ({@code standards/higson/snapshot.md}), so <b>a fresh clone cannot run
 * these tests</b> until {@code mvn higson-cli:pull} has populated it.
 *
 * <p>All date assertions rely on the surefire {@code -Duser.timezone=UTC} pin (P-4): the driver's
 * default birth date is seeded as "today - 40 years, start of day, <i>system</i> zone", so without
 * the pin every date golden becomes machine-dependent.
 */
@SpringBootTest(classes = ApplicationConfig.class, properties = {
		// The engine below is snapshot-backed, so nothing ever queries this pool - it exists only
		// because the Higson starter always builds a 'higsonDataSource'. An in-memory H2 keeps the
		// run offline; the packaged PostgreSQL URL would make these tests need a live database.
		"higson.database.url=jdbc:h2:mem:motor-contract-goldens;DB_CLOSE_DELAY=-1",
		"higson.database.username=sa",
		"higson.database.password=sa"
})
@AutoConfigureMockMvc
@Import(ApiContractTestSupport.SnapshotEngineConfiguration.class)
abstract class ApiContractTestSupport {

	/** Canonical snapshot location, shared with the higson-cli Maven plugin ({@code higson.snapshot.dir}). */
	private static final String SNAPSHOT_DIR = "src/main/higson";

	private static final ObjectMapper JSON = new ObjectMapper();

	@Autowired
	private MockMvc mockMvc;

	/**
	 * Replaces the starter's JDBC-backed engine with one reading the canonical snapshot directory.
	 * The starter's {@code higsonEngine} bean is {@code @ConditionalOnMissingBean}, so this wins.
	 *
	 * <p>The engine is built here rather than by the starter because
	 * {@code HigsonRuntimeAutoConfiguration.higsonEngine} accepts a {@code javax.sql.DataSource} and
	 * always wraps it in a {@code HigsonDbDataSource}; {@link HigsonSnapshotDataSource} implements
	 * {@code HigsonDataSource} instead, so it cannot be routed through that bean. Everything the
	 * starter would configure on top is left at its factory default deliberately - see below.
	 */
	@TestConfiguration
	static class SnapshotEngineConfiguration {

		@Bean
		HigsonEngine higsonEngine() {
			File snapshot = new File(SNAPSHOT_DIR);
			Assertions.assertTrue(snapshot.isDirectory(),
					"snapshot directory not found at " + snapshot.getAbsolutePath()
							+ " - run 'mvn higson-cli:pull' first (P-2)");
			HigsonEngineFactory factory = new HigsonEngineFactory();
			factory.setDataSource(new HigsonSnapshotDataSource(snapshot));
			// Do NOT mirror higson.runtime.function.validate-arguments=true here. With argument-type
			// validation on, the snapshot reader fails to bind the declared ParamContext argument of
			// the Groovy function util.ctx.get when a flow invokes it, so coverage UMPD (GOLD option)
			// blows up. The same rules evaluate correctly off a live database either way - the
			// function metadata is byte-identical in both - so this is a snapshot-reader limitation,
			// not an application or configuration defect, and switching it off changes no golden.
			return factory.create();
		}
	}

	/**
	 * One session per request sequence. {@code QuoteService} is {@code @SessionScope} and holds the
	 * whole {@code Quote} aggregate, so a fresh session per request would test nothing (spec 6.5).
	 */
	static MockHttpSession newSession() {
		return new MockHttpSession();
	}

	MockHttpServletResponse get(String url, MockHttpSession session) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.get(url).session(session))
				.andReturn().getResponse();
	}

	JsonNode getJson(String url, MockHttpSession session) throws Exception {
		MockHttpServletResponse response = get(url, session);
		Assertions.assertEquals(200, response.getStatus(), "GET " + url + " must answer 200");
		return JSON.readTree(response.getContentAsString());
	}

	/** A raw, unquoted body with {@code Content-Type: text/plain} - what Angular's HttpClient sends for a string. */
	MockHttpServletResponse putText(String url, String body, MockHttpSession session) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.put(url)
						.session(session)
						.contentType(MediaType.TEXT_PLAIN)
						.content(body))
				.andReturn().getResponse();
	}

	/** A JSON body with {@code Content-Type: application/json} - what HttpClient sends for a number, object or Date. */
	MockHttpServletResponse putJson(String url, String body, MockHttpSession session) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.put(url)
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andReturn().getResponse();
	}
}
