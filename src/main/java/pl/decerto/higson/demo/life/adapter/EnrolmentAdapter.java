package pl.decerto.higson.demo.life.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.Enrolment;

/** Wystawia jedno przystąpienie — kod wariantu i liczność, bez żadnych danych osobowych. */
public class EnrolmentAdapter extends Adapter {

	private final Enrolment enrolment;

	public EnrolmentAdapter(Enrolment enrolment) {
		this.enrolment = enrolment;
	}

	@Override
	protected Mapping getMapping() {
		if (enrolment == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("variantCode", enrolment.getVariantCode())
			.add("count", enrolment.getCount());
	}
}
