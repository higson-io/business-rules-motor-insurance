package pl.decerto.hyperon.demo.motor.domain;

public class Address {

	private String city;
	private String street;
	private String zipCode;

	public Address(String city, String street, String zipCode) {
		this.city = city;
		this.street = street;
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public String getStreet() {
		return street;
	}

	public String getZipCode() {
		return zipCode;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Address{");
		sb.append("city='").append(city).append('\'');
		sb.append(", street='").append(street).append('\'');
		sb.append(", zipCode='").append(zipCode).append('\'');
		sb.append('}');
		return sb.toString();
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}
