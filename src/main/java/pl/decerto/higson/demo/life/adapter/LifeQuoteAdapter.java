package pl.decerto.higson.demo.life.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.CollectionAdapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;

/** Korzeń modelu kanonicznego — wszystkie ścieżki quote.* zaczynają się tutaj. */
public class LifeQuoteAdapter extends Adapter {

	private final LifeQuote quote;

	public LifeQuoteAdapter(LifeQuote quote) {
		this.quote = quote;
	}

	@Override
	protected Mapping getMapping() {
		if (quote == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("lineCode", quote.getLineCode())
			.add("insured", new InsuredAdapter(quote.getInsured(), quote.getPolicy().getStartDate()))
			.add("policy", new PolicyAdapter(quote.getPolicy()))
			.add("group", new GroupSchemeAdapter(quote.getGroup()))
			// tylko wybrane umowy — reguły wyceny iterują po tym, co klient faktycznie kupuje
			.add("riders", new CollectionAdapter(quote.getSelectedRiders(),
				rider -> new RiderAdapter((Rider) rider)));
	}
}
