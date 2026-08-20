package pl.decerto.higson.demo.life.adapter;

import java.time.LocalDate;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.Insured;

/**
 * Wystawia osobę ubezpieczoną pod ścieżką quote.insured.*
 *
 * entryAge i bmi są wyliczane, bo tabele przedziałowe czytają je jako gotowe klucze wejściowe —
 * silnik nie wylicza własnych kluczy.
 */
public class InsuredAdapter extends Adapter {

	private final Insured insured;
	private final LocalDate policyStart;

	public InsuredAdapter(Insured insured, LocalDate policyStart) {
		this.insured = insured;
		this.policyStart = policyStart;
	}

	@Override
	protected Mapping getMapping() {
		if (insured == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("birthDate", insured.getBirthDate())
			.add("entryAge", insured.getEntryAge(policyStart))
			.add("smoker", insured.isSmoker())
			.add("heightCm", insured.getHeightCm())
			.add("weightKg", insured.getWeightKg())
			.add("bmi", insured.getBmi())
			.add("occupationClass", insured.getOccupationClass())
			.add("monthlyIncome", insured.getMonthlyIncome())
			.add("priorCoverFrom", insured.getPriorCoverFrom())
			.add("priorCoverTo", insured.getPriorCoverTo());
	}
}
