package pl.decerto.hyperon.demo.motor.api.dto;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DriverDto {
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	private String gender;
	private AddressDto address;
	private int numberOfAccidents;
	private int numberOfTickets;
	private int licenceObtainedAtAge;

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getGender() {
		return gender;
	}

	public void setAddress(AddressDto address) {
		this.address = address;
	}

	public AddressDto getAddress() {
		return address;
	}

	public int getLicenceObtainedAtAge() {
		return licenceObtainedAtAge;
	}

	public void setLicenceObtainedAtAge(int licenceObtainedAtAge) {
		this.licenceObtainedAtAge = licenceObtainedAtAge;
	}

	public int getNumberOfAccidents() {
		return numberOfAccidents;
	}

	public void setNumberOfAccidents(int numberOfAccidents) {
		this.numberOfAccidents = numberOfAccidents;
	}

	public int getNumberOfTickets() {
		return numberOfTickets;
	}

	public void setNumberOfTickets(int numberOfTickets) {
		this.numberOfTickets = numberOfTickets;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("firstName", firstName)
				.append("lastName", lastName)
				.append("dateOfBirth", dateOfBirth)
				.append("gender", gender)
				.append("address", address)
				.append("numberOfAccidents", numberOfAccidents)
				.append("numberOfTickets", numberOfTickets)
				.append("licenceObtainedAtAge", licenceObtainedAtAge)
				.toString();
	}
}
