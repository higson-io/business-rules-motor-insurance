package pl.decerto.higson.demo.motor.adapter;

import java.util.Date;

import pl.decerto.higson.demo.motor.domain.Driver;
import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import io.higson.runtime.rhino.RhinoDate;

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
