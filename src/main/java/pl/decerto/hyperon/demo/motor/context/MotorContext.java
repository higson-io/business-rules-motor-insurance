package pl.decerto.hyperon.demo.motor.context;

import org.apache.commons.lang3.StringUtils;

import pl.decerto.hyperon.demo.motor.adapter.CoverageAdapter;
import pl.decerto.hyperon.demo.motor.adapter.OptionAdapter;
import pl.decerto.hyperon.demo.motor.adapter.QuoteAdapter;
import pl.decerto.hyperon.demo.motor.domain.Coverage;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.demo.motor.domain.Option;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.ext.adapter.Adapter;
import pl.decerto.hyperon.runtime.core.HyperonContext;

public class MotorContext extends HyperonContext {

	private Quote quote;
	private Coverage coverage;
	private Driver driver;
	private Option option;


	public MotorContext(Object... args) {
		super(args);
	}

	public MotorContext(Coverage coverage) {
		this.coverage = coverage;
	}

	public MotorContext(Option option) {
		this.option = option;
	}

	/*
	 * PART 1 :: dynamic context
	 */

	public Coverage getCoverage() {
		return coverage;
	}

	public Option getOption() {

		if (option == null && getCoverage() != null) {
			option = getCoverage().getOption();
		}

		return option;
	}

	public Quote getQuote() {

		if (quote == null && getCoverage() != null) {
			quote = getCoverage().getQuote();
		}

		if (quote == null && getOption() != null) {
			quote = getOption().getQuote();
		}

		return quote;
	}

	public Driver getDriver() {
		if (driver == null && getQuote() != null) {
			driver = getQuote().getDriver();
		}
		return driver;
	}


	/*
	 * PART 2 :: canonical model adapter
	 */

	/**
	 * get value from path,
	 * where path is valid path in canonical model,
	 * for example:
	 * - quote.insured.professionCode
	 * - quote.insured.age
	 * - quote.productCode
	 * - cover.code
	 */
	@Override
	public Object get(String path) {
		String name = getFirstToken(path);
		String subpath = skipFirstToken(path);

		Adapter adapter = null;
		if (name.equals("quote")) {
			adapter = new QuoteAdapter(getQuote());
		}

		if (name.equals("coverage")) {
			adapter = new CoverageAdapter(getCoverage());
		}

		if (name.equals("option")) {
			adapter = new OptionAdapter(getOption());
		}

		if (adapter != null) {
			return adapter.get(subpath);
		}

		return null;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public void setCoverage(Coverage coverage) {
		this.coverage = coverage;
	}

	private String getFirstToken(String path) {
		return StringUtils.substringBefore(path, ".");
	}

	private String skipFirstToken(String path) {
		return StringUtils.substringAfter(path, ".");
	}

}
