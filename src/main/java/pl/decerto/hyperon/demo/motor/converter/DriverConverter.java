package pl.decerto.hyperon.demo.motor.converter;

import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pl.decerto.hyperon.demo.motor.domain.Address;
import pl.decerto.hyperon.demo.motor.domain.Driver;
import pl.decerto.hyperon.demo.motor.api.dto.AddressDto;
import pl.decerto.hyperon.demo.motor.api.dto.DriverDto;

@Component
public class DriverConverter implements Converter<Driver, DriverDto> {

	@Override
	public DriverDto convert(Driver driver) {
		Objects.requireNonNull(driver, "Driver can not be null.");
		DriverDto dto = new DriverDto();
		dto.setFirstName(driver.getFirstName());
		dto.setLastName(driver.getLastName());
		dto.setDateOfBirth(driver.getDateOfBirth());
		dto.setGender(driver.getGender());
		dto.setNumberOfTickets(ObjectUtils.firstNonNull(driver.getNumberOfTickets(), 0));
		dto.setNumberOfAccidents(ObjectUtils.firstNonNull(driver.getNumberOfAccidents(), 0));
		dto.setLicenceObtainedAtAge(ObjectUtils.firstNonNull(driver.getLicenceObtainedAtAge(), 0));
		dto.setAddress(convertAddress(driver.getAddress()));
		return dto;
	}

	private AddressDto convertAddress(Address address) {
		Objects.requireNonNull(address, "Address can not be null.");
		AddressDto dto = new AddressDto();
		dto.setZipCode(address.getZipCode());
		dto.setCity(address.getCity());
		dto.setStreet(address.getStreet());
		return dto;
	}
}
