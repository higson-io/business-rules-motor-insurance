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
 * ⛔ **Każda dana ma być NIEMOŻLIWA, a nie tylko wymyślona.** Wymyślony numer, który przypadkiem
 * jest poprawny, może należeć do prawdziwej osoby — i wtedy demo pokazuje na rzutniku czyjeś dane.
 * Dlatego:
 *
 * • **PESEL ma CELOWO błędną sumę kontrolną.** `12345678901` wymaga cyfry kontrolnej 3, a kończy
 *   się na 1, więc nie jest i nie może być niczyim numerem. To odwrotność pierwszego odruchu
 *   („niech wygląda poprawnie"), i to ten odruch był tu błędem. ⚠️ Uwaga na `00000000000` —
 *   wygląda na oczywisty wypełniacz, a sumę kontrolną ma POPRAWNĄ.
 * • **Telefon ma prefiks `000`**, którego polski plan numeracji nie przydziela.
 * • **Kod pocztowy `00-000`** nie jest przypisany do żadnej miejscowości.
 * • **Domena `example.com`** jest zarezerwowana przez RFC 2606 właśnie do takich zastosowań.
 * • Ulica, numer dowodu i nazwisko są czytelnymi wypełniaczami, a nie prawdopodobnymi danymi.
 *
 * Formularz wygląda przez to na demonstracyjny, i o to chodzi: na prezentacji nikt nie ma prawa
 * zastanawiać się, czyje to dane.
 */
public class Policyholder {

	private String firstName = "Anna";
	private String lastName = "Przykładowa";
	private String pesel = "12345678901";
	private LocalDate birthDate = LocalDate.of(1990, 1, 1);
	private String idDocument = "ABC 123456";

	private String street = "ul. Przykładowa 1";
	private String flat = "2";
	private String postcode = "00-000";
	private String city = "Warszawa";

	private String email = "anna.przykladowa@example.com";
	private String phone = "+48 000 000 000";

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
