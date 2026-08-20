package pl.decerto.higson.demo.life.context;

import io.higson.runtime.core.HigsonContext;
import io.higson.runtime.ext.adapter.Adapter;
import org.apache.commons.lang3.StringUtils;

import pl.decerto.higson.demo.life.adapter.LifeQuoteAdapter;
import pl.decerto.higson.demo.life.adapter.RiderAdapter;
import pl.decerto.higson.demo.life.domain.LifeQuote;
import pl.decerto.higson.demo.life.domain.Rider;

/**
 * Kontekst profilu LIFE — jedyne miejsce, które wie, że ścieżka "quote.insured.entryAge"
 * rozwiązuje się przez adapter, a "rider.code" przez element iteracji.
 *
 * Elementy iteracji (rider, variant, benefit, claim) są adresowane nazwą typu, dokładnie tak jak
 * coverage i option w profilu motor. Nie występują na liście ścieżek kontekstu i to jest w porządku:
 * lista wystawia wyłącznie statyczne drzewo.
 */
public class LifeContext extends HigsonContext {

	private LifeQuote quote;
	private Rider rider;
	private String variantCode;
	private String benefitCode;
	private String claimCause;

	public LifeContext(LifeQuote quote) {
		this.quote = quote;
	}

	public LifeContext(LifeQuote quote, Rider rider) {
		this.quote = quote;
		this.rider = rider;
	}

	public LifeContext withVariant(String variantCode) {
		this.variantCode = variantCode;
		return this;
	}

	public LifeContext withBenefit(String benefitCode) {
		this.benefitCode = benefitCode;
		return this;
	}

	/** Przyczyna zdarzenia. Karencja nie obowiązuje przy nieszczęśliwym wypadku — zapis z OWU. */
	public LifeContext withClaimCause(String claimCause) {
		this.claimCause = claimCause;
		return this;
	}

	@Override
	public Object get(String path) {
		String name = StringUtils.substringBefore(path, ".");
		String subpath = StringUtils.substringAfter(path, ".");

		Adapter adapter = null;

		if ("quote".equals(name)) {
			adapter = new LifeQuoteAdapter(quote);
		}

		if ("rider".equals(name)) {
			adapter = new RiderAdapter(rider);
		}

		if ("variant".equals(name) && "code".equals(subpath)) {
			return variantCode;
		}

		if ("benefit".equals(name) && "code".equals(subpath)) {
			return benefitCode;
		}

		if ("claim".equals(name) && "cause".equals(subpath)) {
			return claimCause;
		}

		if (adapter != null) {
			return adapter.get(subpath);
		}

		return null;
	}

	public LifeQuote getQuote() {
		return quote;
	}

	public Rider getRider() {
		return rider;
	}
}
