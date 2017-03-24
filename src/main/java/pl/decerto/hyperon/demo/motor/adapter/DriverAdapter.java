package pl.decerto.hyperon.demo.motor.adapter;

import java.util.Date;

import pl.decerto.hyperon.ext.adapter.Adapter;
import pl.decerto.hyperon.ext.adapter.Mapping;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.runtime.rhino.RhinoDate;

public class DriverAdapter extends Adapter {

	private Driver driver;

	public DriverAdapter(Driver driver) {
		this.driver = driver;
	}

	@Override
	protected Mapping getMapping() {
		return new Mapping()
			.add("firstname", driver.getFirstName())
			.add("lastname", driver.getLastName())
			.add("dateofbirth", driver.getDateOfBirth())
			.add("gender", driver.getGender())
			.add("age", computeAge())
			.add("numberOfAccidents", driver.getNumberOfAccidents())
			.add("numberOfTickets", driver.getNumberOfTickets())
			.add("licenceObtainedAtAge", driver.getLicenceObtainedAtAge())
			.add("address", driver.getAddress() != null ? new AddressAdapter(driver.getAddress()) : null);
	}

	private int computeAge() {
		return RhinoDate.getAbsoluteYearDiff(driver.getDateOfBirth(), new Date());
	}
}
