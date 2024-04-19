package pl.decerto.higson.demo.motor.domain;

import java.util.ArrayList;
import java.util.List;

public class Option {

	private final String code;
	private final List<Coverage> coverages;
	private final List<Discount> discounts;
	private final int order;
	private Quote quote;

	public Option(String code, int order) {
		this.code = code;
		this.order = order;
		this.coverages = new ArrayList<>();
		this.discounts = new ArrayList<>();
	}

	public String getCode() {
		return code;
	}

	public List<Coverage> getCoverages() {
		return coverages;
	}

	public void addCoverage(Coverage c) {
		c.setOption(this);
		coverages.add(c);
	}

	public void addDiscount(Discount d) {
		discounts.add(d);
	}

	public Quote getQuote() {
		return quote;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	public int getOrder() {
		return order;
	}

	public List<Discount> getDiscounts() {
		return discounts;
	}

	public void clearDiscounts() {
		discounts.clear();
	}
}
