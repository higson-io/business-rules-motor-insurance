package pl.decerto.higson.demo.life.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.Policy;

/** Wystawia parametry polisy pod ścieżką quote.policy.* */
public class PolicyAdapter extends Adapter {

	private final Policy policy;

	public PolicyAdapter(Policy policy) {
		this.policy = policy;
	}

	@Override
	protected Mapping getMapping() {
		if (policy == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("startDate", policy.getStartDate())
			.add("mainSum", policy.getMainSum())
			.add("paymentFrequency", policy.getPaymentFrequency());
	}
}
