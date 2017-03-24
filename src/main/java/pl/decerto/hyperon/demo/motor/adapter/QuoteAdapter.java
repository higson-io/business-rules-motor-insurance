package pl.decerto.hyperon.demo.motor.adapter;

import pl.decerto.hyperon.demo.motor.domain.Option;
import pl.decerto.hyperon.demo.motor.domain.Quote;
import pl.decerto.hyperon.ext.adapter.Adapter;
import pl.decerto.hyperon.ext.adapter.CollectionAdapter;
import pl.decerto.hyperon.ext.adapter.Mapping;

public class QuoteAdapter extends Adapter {

	private Quote quote;

	public QuoteAdapter(Quote quote) {
		this.quote = quote;
	}

	@Override
	protected Mapping getMapping() {
		return new Mapping()
			.add("planCode", quote != null ? quote.getPlan() : null)
			.add("driver", new DriverAdapter(quote.getDriver()))
			.add("vehicle", new VehicleAdapter(quote.getVehicle()))
			.add("options", new CollectionAdapter(quote.getOptions(),
				option -> new OptionAdapter((Option) option)
			));

	}

}
