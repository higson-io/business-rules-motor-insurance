package pl.decerto.higson.demo.motor.converter;

import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.decerto.higson.demo.motor.domain.Driver;
import pl.decerto.higson.demo.motor.api.dto.AddressDto;
import pl.decerto.higson.demo.motor.api.dto.DriverDto;
import pl.decerto.higson.demo.motor.domain.Address;

public class DriverConverterTest {
	private DriverConverter driverConverter;

	@Before
	public void setUp() throws Exception {
		driverConverter = new DriverConverter();
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenDriverIsNull() throws Exception {
		driverConverter.convert(null);
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenAddressIsNull() throws Exception {
		Driver driver = new Driver();
		driverConverter.convert(driver);
	}

	@Test
	public void shouldConvertDriver() throws Exception {
		//given
		Driver driver = createDriver();

		//when
		DriverDto result = driverConverter.convert(driver);

		//then
		Assert.assertNotNull(result);
		Assert.assertEquals(result.getFirstName(), driver.getFirstName());
		Assert.assertEquals(result.getGender(), driver.getGender());
		Assert.assertEquals(result.getLastName(), driver.getLastName());
		Assert.assertNotNull(driver.getNumberOfAccidents());
		Assert.assertEquals(result.getNumberOfAccidents(), (int) driver.getNumberOfAccidents());
		Assert.assertNotNull(driver.getNumberOfTickets());
		Assert.assertEquals(result.getNumberOfTickets(), (int) driver.getNumberOfTickets());
		Assert.assertNotNull(driver.getLicenceObtainedAtAge());
		Assert.assertEquals(result.getLicenceObtainedAtAge(), (int) driver.getLicenceObtainedAtAge());
		assertDateEquality(result.getDateOfBirth(), driver.getDateOfBirth());
		assertAddressEquality(result.getAddress(), driver.getAddress());
	}

	private Driver createDriver() {
		Driver driver = new Driver();
		driver.setFirstName("John");
		driver.setLastName("Snow");
		driver.setGender("F");
		driver.setDateOfBirth(new Date(LocalDate.of(1980, Month.APRIL, 1).toEpochDay()));
		driver.setNumberOfAccidents(0);
		driver.setNumberOfTickets(1);
		driver.setLicenceObtainedAtAge(20);
		Address address = new Address("New York", "First street", "12121");
		driver.setAddress(address);
		return driver;
	}

	private static void assertDateEquality(Date targetDate, Date sourceDate) {
		Assert.assertNotNull(targetDate);
		LocalDate target = LocalDate.ofEpochDay(targetDate.getTime());
		LocalDate source = LocalDate.ofEpochDay(sourceDate.getTime());
		Assert.assertEquals(source.getYear(), target.getYear());
		Assert.assertEquals(source.getMonthValue(), target.getMonthValue());
		Assert.assertEquals(source.getDayOfMonth(), target.getDayOfMonth());
	}

	private static void assertAddressEquality(AddressDto target, Address source) {
		Assert.assertNotNull(target);
		Assert.assertEquals(source.getCity(), target.getCity());
		Assert.assertEquals(source.getStreet(), target.getStreet());
		Assert.assertEquals(source.getZipCode(), target.getZipCode());
	}
}