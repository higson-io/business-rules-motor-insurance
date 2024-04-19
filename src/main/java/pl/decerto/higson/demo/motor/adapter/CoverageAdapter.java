package pl.decerto.higson.demo.motor.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.motor.domain.Coverage;

public class CoverageAdapter extends Adapter {

	private Coverage coverage;

	public CoverageAdapter(Coverage coverage) {
		this.coverage = coverage;
	}

	@Override
	public Mapping getMapping() {
		return new Mapping()

			// simple types (model leaves)
			.add("code", coverage.getCode())
			.add("limit1", coverage.getLimit1())
			.add("limit2", coverage.getLimit2())
			.add("premium", coverage.getPremium())

			// parent option (there is exactly one parent option)
			.add("option", new OptionAdapter(coverage.getOption()))

			// parent quote (there is exactly one parent quote)
			.add("quote", new QuoteAdapter(coverage.getQuote()));
	}

}
