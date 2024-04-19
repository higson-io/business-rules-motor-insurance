package pl.decerto.higson.demo.motor.service;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.decerto.higson.demo.motor.domain.Driver;

@Service
public class DriverService {
	private final QuoteService quoteService;

	@Autowired
	public DriverService(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	public Driver getDriver() {
		return quoteService.getQuote().getDriver();
	}

	public void updateFirstName(String firstName) {
		getDriver().setFirstName(firstName);
	}

	public void updateGender(String gender) {
		getDriver().setGender(gender);
	}

	public void updateLastName(String lastName) {
		getDriver().setLastName(lastName);
	}

	public void updateBirthDate(Date birthDate) {
		getDriver().setDateOfBirth(birthDate);
	}

	public void updateZipCode(String zipCode) {
		getDriver().getAddress().setZipCode(zipCode);
	}

	public void updateCity(String city) {
		getDriver().getAddress().setCity(city);
	}

	public void updateStreet(String street) {
		getDriver().getAddress().setStreet(street);
	}

	public void updateAccidentCount(Integer accidentCount) {
		getDriver().setNumberOfAccidents(accidentCount);
	}

	public void updateTrafficTicketsCount(Integer trafficTicketsCount) {
		getDriver().setNumberOfTickets(trafficTicketsCount);
	}

	public void licenceObtainedAtAge(Integer licenceObtainedAtAge) {
		getDriver().setLicenceObtainedAtAge(licenceObtainedAtAge);
	}
}
