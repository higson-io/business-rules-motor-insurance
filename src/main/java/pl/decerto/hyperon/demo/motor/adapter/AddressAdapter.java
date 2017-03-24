package pl.decerto.hyperon.demo.motor.adapter;

import pl.decerto.hyperon.ext.adapter.Adapter;
import pl.decerto.hyperon.ext.adapter.Mapping;
import pl.decerto.hyperon.demo.motor.domain.Address;

public class AddressAdapter extends Adapter {

	private Address address;

	public AddressAdapter(Address address) {
		this.address = address;
	}

	@Override
	protected Mapping getMapping() {
		return new Mapping()
			.add("city", address.getCity())
			.add("street", address.getStreet())
			.add("zipcode", address.getZipCode());
	}

}
