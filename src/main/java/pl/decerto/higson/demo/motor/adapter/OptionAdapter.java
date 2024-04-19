package pl.decerto.higson.demo.motor.adapter;

import pl.decerto.higson.demo.motor.domain.Option;
import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.CollectionAdapter;
import io.higson.runtime.ext.adapter.Mapping;

public class OptionAdapter extends Adapter {

	private Option option;

	public OptionAdapter(Option option) {
		this.option = option;
	}

	@Override
	public Mapping getMapping() {
		return new Mapping()
			.add("code", option.getCode())
			.add("coverages", new CollectionAdapter(option.getCoverages(), CoverageAdapter.class))
			.add("quote", new QuoteAdapter(option.getQuote()));
	}

}
