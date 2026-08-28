package pl.decerto.higson.demo.property.api.dto;

import java.time.LocalDate;

/** Ubezpieczający na drucie. Dane demonstracyjne — patrz `Policyholder`. */
public class PropertyPolicyholderDto {

	private String firstName;
	private String lastName;
	private String pesel;
	private LocalDate birthDate;
	private String idDocument;
	private String street;
	private String flat;
	private String postcode;
	private String city;
	private String email;
	private String phone;
	private String source;

	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }
	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }
	public String getPesel() { return pesel; }
	public void setPesel(String pesel) { this.pesel = pesel; }
	public LocalDate getBirthDate() { return birthDate; }
	public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
	public String getIdDocument() { return idDocument; }
	public void setIdDocument(String idDocument) { this.idDocument = idDocument; }
	public String getStreet() { return street; }
	public void setStreet(String street) { this.street = street; }
	public String getFlat() { return flat; }
	public void setFlat(String flat) { this.flat = flat; }
	public String getPostcode() { return postcode; }
	public void setPostcode(String postcode) { this.postcode = postcode; }
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getSource() { return source; }
	public void setSource(String source) { this.source = source; }
}
