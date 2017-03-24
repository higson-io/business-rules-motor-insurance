package pl.decerto.hyperon.demo.motor.api;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.decerto.hyperon.demo.motor.converter.DriverConverter;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.demo.motor.api.dto.DriverDto;
import pl.decerto.hyperon.demo.motor.service.DriverService;

@RestController
@RequestMapping("/driver")
public class DriverApi {

	private final DriverService driverService;
	private final DriverConverter driverConverter;

	@Autowired
	public DriverApi(DriverService driverService, DriverConverter driverConverter) {
		this.driverService = driverService;
		this.driverConverter = driverConverter;
	}

	@GetMapping
	public DriverDto getDriver() {
		Driver driver = driverService.getDriver();
		return driverConverter.convert(driver);
	}

	@PutMapping("/birthDate")
	public void updateBirthDate(@RequestBody Date birthDate) {
		driverService.updateBirthDate(birthDate);
	}

	@PutMapping("/firstName")
	public void updateFirstName(@RequestBody(required = false) String firstName) {
		driverService.updateFirstName(firstName);
	}

	@PutMapping("/gender")
	public void updateGender(@RequestBody(required = false) String gender) {
		driverService.updateGender(gender);
	}

	@PutMapping("/lastName")
	public void updateLastName(@RequestBody(required = false) String lastName) {
		driverService.updateLastName(lastName);
	}

	@PutMapping("/accidentCount")
	public void updateAccidentCount(@RequestBody(required = false) Integer accidentCount) {
		driverService.updateAccidentCount(accidentCount);
	}

	@PutMapping("/trafficTicketsCount")
	public void updateTrafficTicketsCount(@RequestBody(required = false) Integer trafficTicketsCount) {
		driverService.updateTrafficTicketsCount(trafficTicketsCount);
	}

	@PutMapping("/licenceObtainedAtAge")
	public void updateLicenceObtainedAtAge(@RequestBody(required = false) Integer licenceObtainedAtAge) {
		driverService.licenceObtainedAtAge(licenceObtainedAtAge);
	}

	@PutMapping("/address/zipCode")
	public void updateZipCode(@RequestBody(required = false) String zipCode) {
		driverService.updateZipCode(zipCode);
	}

	@PutMapping("/address/city")
	public void updateCity(@RequestBody(required = false) String city) {
		driverService.updateCity(city);
	}

	@PutMapping("/address/street")
	public void updateStreet(@RequestBody(required = false) String street) {
		driverService.updateStreet(street);
	}
}
