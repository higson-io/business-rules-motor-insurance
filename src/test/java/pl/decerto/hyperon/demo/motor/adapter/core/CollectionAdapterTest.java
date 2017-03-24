package pl.decerto.hyperon.demo.motor.adapter.core;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import pl.decerto.hyperon.demo.motor.adapter.CoverageAdapter;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.ext.adapter.CollectionAdapter;
import pl.decerto.hyperon.runtime.core.HyperonSubContext;

public class CollectionAdapterTest {

	@Test
	public void test() {

		// given

		Coverage c1 = new Coverage("BI", "BI");
		c1.setPremium(new BigDecimal("110"));

		Coverage c2 = new Coverage("PD", "PD");
		c2.setPremium(new BigDecimal("220"));

		List<Coverage> list = Arrays.asList(c1, c2);

		// when

		CollectionAdapter<Coverage> adapter = new CollectionAdapter<>(list, CoverageAdapter::new);

		// then

		assertEquals(adapter.size(), 2);

		int total = 0;
		for (HyperonSubContext e : adapter) {
			total += e.getInteger("premium");
		}

		assertEquals(total, 330);
	}

	@Test
	public void shouldCreateCollectionAdapterWithGenericFactory() {

		Coverage c1 = new Coverage("BI", "BI");
		c1.setPremium(new BigDecimal(50));
		Coverage c2 = new Coverage("PD", "PD");
		c2.setPremium(new BigDecimal(60));

		List<Coverage> list = Arrays.asList(c1, c2);

		// when

		CollectionAdapter<Coverage> adapter = new CollectionAdapter<>(list, CoverageAdapter.class);

		// then

		assertEquals(adapter.size(), 2);

		int total = 0;
		for (HyperonSubContext e : adapter) {
			total += e.getInteger("premium");
		}

		assertEquals(total, 110);
	}

	@Test
	public void shouldCreateCollectionAdapterWithEmptyCollection() {

		// when

		CollectionAdapter<Coverage> adapter = new CollectionAdapter<>(null, CoverageAdapter.class);

		// then

		assertEquals(adapter.size(), 0);
		assertEquals(adapter.isEmpty(), true);
		assertEquals(adapter.isNotEmpty(), false);
		assertEquals(adapter.iterator().hasNext(), false);
	}
}
