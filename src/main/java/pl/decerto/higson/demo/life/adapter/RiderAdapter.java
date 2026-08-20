package pl.decerto.higson.demo.life.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.Rider;

/** Wystawia umowę dodatkową pod ścieżką rider.* — element iteracji, adresowany nazwą typu. */
public class RiderAdapter extends Adapter {

	private final Rider rider;

	public RiderAdapter(Rider rider) {
		this.rider = rider;
	}

	@Override
	protected Mapping getMapping() {
		if (rider == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("code", rider.getCode())
			.add("sum", rider.getSum())
			.add("premium", rider.getPremium());
	}
}
