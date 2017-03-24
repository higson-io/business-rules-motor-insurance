package pl.decerto.hyperon.demo.motor.adapter.core;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

import pl.decerto.hyperon.demo.motor.adapter.CoverageAdapter;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.ext.adapter.Adapter;
import pl.decerto.hyperon.ext.adapter.AdapterFactory;
import pl.decerto.hyperon.ext.adapter.AdapterFactoryImpl;

public class AdapterFactoryImplTest {

	@Test
	public void shouldCreateAdapter() {

		// given

		AdapterFactory<Coverage> factory = new AdapterFactoryImpl<>(CoverageAdapter.class);
		Coverage pc = new Coverage("BI", "BI");
		pc.setPremium(new BigDecimal(200));

		// when

		Adapter a = factory.create(pc);

		// then

		assertEquals(a.getClass(), CoverageAdapter.class);
		assertEquals(a.get("premium"), new BigDecimal("200"));
	}

	@Test
	public void shouldFailForBadAdapter() {
		AdapterFactory<Coverage> factory = new AdapterFactoryImpl<>(TestBadAdapter.class);
		Coverage pc = new Coverage("BI", "BI");
		pc.setPremium(new BigDecimal(200));

		try {
			factory.create(pc);
			fail();

		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("Failed to create Adapter"));
		}
	}

	@Test
	public void performanceTest() {

		AdapterFactory<Coverage> factory = new AdapterFactoryImpl<>(CoverageAdapter.class);
		Coverage pc = new Coverage("BI", "BI");
		pc.setPremium(new BigDecimal(200));

		long t = System.nanoTime();
		long m = System.currentTimeMillis();

		for (int i = 0; i < 10000; i++) {
			factory.create(pc);
		}

		long time = System.nanoTime() - t;
		long mtime = System.currentTimeMillis() - m;

		out.println("time: " + time / 1000 + " us");
		out.println("time: " + mtime + " ms");

		/*
			factory.create  x  10k
			================================
				reflection no cache  18 ms
				reflection cache      7 ms
				direct call           2 ms
		 */

	}


	private static class TestBadAdapter extends Adapter {

		// no constructor or bad constructor
	}

}
