package pl.decerto.higson.demo.property.service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * Numery oferty i polisy na potrzeby demo.
 *
 * ⛔ **To NIE jest reguła biznesowa i celowo nie ma jej w Higsonie.** Numer dokumentu to
 * identyfikator: nie zależy od odpowiedzi klienta, nie zmienia ceny i nie ma wersji datą. Wrzucenie
 * go do tabeli decyzyjnej rozmyłoby granicę, którą całe to demo pokazuje — że w regułach siedzi to,
 * co rozstrzyga o produkcie, a nie wszystko, co da się skonfigurować.
 *
 * ⚠️ Licznik żyje w pamięci i startuje od nowa razem z aplikacją. Prawdziwy system bierze numer
 * z sekwencji w bazie; tutaj wystarczy, żeby dwie oferty w jednej prezentacji miały różne numery.
 */
@Component
public class DocumentNumbers {

	private final AtomicLong sequence = new AtomicLong(1);

	public String nextOffer(LocalDate issuedOn) {
		return format("OF", issuedOn, sequence.getAndIncrement());
	}

	/** Polisa dziedziczy numer po ofercie, z której powstała — to ten sam dokument. */
	public String policyFor(String offerNumber) {
		return offerNumber.replaceFirst("^OF/", "POL/");
	}

	private String format(String prefix, LocalDate date, long value) {
		return String.format("%s/%d/%06d", prefix, date.getYear(), value);
	}
}
