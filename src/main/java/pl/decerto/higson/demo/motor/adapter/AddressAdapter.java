package pl.decerto.higson.demo.motor.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.motor.domain.Address;

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
