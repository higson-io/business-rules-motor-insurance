package pl.decerto.higson.demo.property.domain;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Ubezpieczający — dane, bez których nie da się zawrzeć umowy.
 *
 * ⛔ **Dane demonstracyjne, zaszyte w kodzie i celowo NIE w regułach.** Kto jest klientem, nie jest
 * decyzją produktową: nie zależy od daty polisy, nie ma wersji i nie zmienia ceny. W działającym
 * wdrożeniu przychodzą z CRM albo z wniosku, a nie z tabeli decyzyjnej — i dokładnie dlatego są
 * tutaj, a nie w Higsonie. Ta granica jest tezą całego demo, więc łamanie jej dla wygody
 * kosztowałoby więcej, niż warte jest jedno pole mniej w Javie.
 *
 * ⚠️ Osoba fikcyjna. PESEL ma poprawną sumę kontrolną, bo formularz z niepoprawnym numerem wygląda
 * na zepsuty, a nie na demonstracyjny; numer dowodu i adres są wymyślone.
 */
public class Policyholder {

	private String firstName = "Anna";
	private String lastName = "Kowalska";
	private String pesel = "85092612346";
	private LocalDate birthDate = LocalDate.of(1985, 9, 26);
	private String idDocument = "AZS 481920";

	private String street = "Marszałkowska 84/92";
	private String flat = "37";
	private String postcode = "00-950";
	private String city = "Warszawa";

	private String email = "anna.kowalska@example.com";
	private String phone = "+48 601 234 567";

	/**
	 * Rek. 8.3 — skąd wzięły się dane, skoro klient ich tu nie wpisywał.
	 *
	 * Ta sama zasada, którą ankieta stosuje do podpowiedzianych odpowiedzi: wartość z innego źródła
	 * niż klient musi się przedstawić.
	 */
	private String source = "Kartoteka klienta (CRM)";

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

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("lastName", lastName).toString();
	}
}
