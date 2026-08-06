package pl.decerto.higson.demo.motor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.core.HigsonEngineFactory;
import io.higson.runtime.core.datasource.snapshot.HigsonSnapshotDataSource;
import io.higson.runtime.engine.core.output.MultiValue;
import io.higson.runtime.engine.core.output.ParamValue;

/**
 * Demo: run the Higson engine straight off a published <b>snapshot</b> (a .zip exported from
 * Higson Studio) instead of a live Studio database.
 *
 * <p>Instead of the usual {@code HigsonDbDataSource} (backed by JDBC), we plug a
 * {@link HigsonSnapshotDataSource} into the {@link HigsonEngineFactory}. The engine then reads
 * parameters/domain/functions directly from the snapshot files - no database, no network, fully
 * self-contained. This is ideal for unit tests, offline evaluation, and reproducible rule runs.
 *
 * <p>The snapshot lives in {@code src/main/higson} - the same directory the higson-cli Maven
 * plugin reads/writes ({@code mvn higson-cli:pull} / {@code higson-cli:push}). So the rules this
 * test runs against are exactly what you last pulled from (or pushed to) Higson Studio.
 * {@link HigsonSnapshotDataSource} accepts either a .zip or an unpacked directory - here it is the
 * directory produced by the CLI.
 */
class SnapshotDataSourceDemoTest {

	private static final Logger LOG = LoggerFactory.getLogger(SnapshotDataSourceDemoTest.class);

	/**
	 * Canonical snapshot location, shared with the higson-cli Maven plugin ({@code higson.snapshot.dir}).
	 * Relative to the project base directory (Maven runs tests from there).
	 */
	private static final String SNAPSHOT_DIR = "src/main/higson";

	private static HigsonEngine engine;

	@BeforeAll
	static void bootEngineFromSnapshot() {
		File snapshot = snapshotFile();

		// 1) the snapshot directory becomes the engine's data source (SnapshotDirReader is used
		//    for a folder; a ".zip" file would use SnapshotZipReader instead)
		HigsonSnapshotDataSource snapshotDataSource = new HigsonSnapshotDataSource(snapshot);

		// 2) plug it into the factory exactly where a JDBC HigsonDbDataSource would normally go
		HigsonEngineFactory factory = new HigsonEngineFactory();
		factory.setDataSource(snapshotDataSource);

		// 3) build a fully working, read-only engine backed by the snapshot
		engine = factory.create();
	}

	@Test
	void loadsSnapshotAndReadsParameterWithoutContext() {
		// demo.motor.dict.productionYear has no input levels -> empty context is enough
		ParamValue productionYears = engine.get("demo.motor.dict.productionYear", new HigsonContext());

		List<MultiValue> rows = productionYears.rows();
		assertFalse(rows.isEmpty(), "expected production-year rows loaded from the snapshot");

		logRows("demo.motor.dict.productionYear", rows, "code", "name");

		// output levels of this parameter are 'code' and 'name' (both the year)
		MultiValue first = rows.get(0);
		assertNotNull(first.getString("code"));
		assertNotNull(first.getString("name"));
	}

	@Test
	void readsParameterWithContext() {
		// demo.motor.dict.vehicle.availableMakes takes production_year as input;
		// the value source (context key) is quote.vehicle.productionYear (see DictionaryService)
		HigsonContext context = new HigsonContext("quote.vehicle.productionYear", 2020);

		ParamValue makes = engine.get("demo.motor.dict.vehicle.availableMakes", context);

		List<MultiValue> rows = makes.rows();
		assertFalse(rows.isEmpty(), "expected available-makes rows for the given production year");

		logRows("demo.motor.dict.vehicle.availableMakes", rows, "make_id", "make");

		// output levels are 'make' (label) and 'make_id'
		MultiValue first = rows.get(0);
		assertNotNull(first.getString("make"));
		assertNotNull(first.getString("make_id"));
	}

	/** Dumps every row returned by the engine to the log so a run visibly proves data came back. */
	private static void logRows(String parameter, List<MultiValue> rows, String... columns) {
		LOG.info("=== '{}' returned {} row(s) from the snapshot ===", parameter, rows.size());
		int index = 0;
		for (MultiValue row : rows) {
			StringBuilder line = new StringBuilder();
			for (String column : columns) {
				if (line.length() > 0) {
					line.append(", ");
				}
				line.append(column).append('=').append(row.getString(column));
			}
			LOG.info("  row[{}] {}", index++, line);
		}
	}

	@Test
	void snapshotDataSourceReportsItsOrigin() {
		// sanity: the engine really is backed by the snapshot directory, not a database
		HigsonSnapshotDataSource snapshotDataSource = new HigsonSnapshotDataSource(snapshotFile());
		assertTrue(snapshotDataSource.datasourceInfo().contains("higson"),
				"datasourceInfo should point at the snapshot directory");
	}

	private static File snapshotFile() {
		File snapshotDir = new File(SNAPSHOT_DIR);
		assertTrue(snapshotDir.isDirectory(),
				"snapshot directory not found at " + snapshotDir.getAbsolutePath()
						+ " - run 'mvn higson-cli:pull' or unpack a snapshot there first");
		return snapshotDir;
	}
}
