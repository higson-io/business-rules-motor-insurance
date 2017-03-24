package pl.decerto.hyperon.demo.motor.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Driver {

	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	private String gender;
	private Address address;

	// number of accidents in the last 5 years
	private int numberOfAccidents;

	// number of traffic tickets in the last 5 years
	private int numberOfTickets;

	// driver's age when driver licence was obtained
	private int licenceObtainedAtAge;

	public String getFirstName() {
		return firstName;
	}

	public Driver setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public Driver setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public Driver setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		return this;
	}

	public Address getAddress() {
		return address;
	}

	public Driver setAddress(Address address) {
		this.address = address;
		return this;
	}

	public String getGender() {
		return gender;
	}

	public Driver setGender(String gender) {
		this.gender = gender;
		return this;
	}

	public int getLicenceObtainedAtAge() {
		return licenceObtainedAtAge;
	}

	public Driver setLicenceObtainedAtAge(int licenceObtainedAtAge) {
		this.licenceObtainedAtAge = licenceObtainedAtAge;
		return this;
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
