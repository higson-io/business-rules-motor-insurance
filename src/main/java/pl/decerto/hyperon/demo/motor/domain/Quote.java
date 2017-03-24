package pl.decerto.hyperon.demo.motor.domain;

import java.util.ArrayList;
import java.util.List;

import org.smartparam.engine.util.Formatter;

public class Quote {

	private String plan;
	private Driver driver = new Driver();
	private Vehicle vehicle = new Vehicle();
	private List<Option> options = new ArrayList<>();

	public Quote() {
	}

	public Quote(String plan, Driver driver) {
		this.plan = plan;
		this.driver = driver;
	}

	public Driver getDriver() {
		return driver;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public void addOption(Option o) {
		o.setQuote(this);
		options.add(o);
	}

	public String getPlan() {
		return plan;
	}

	public List<Option> getOptions() {
		return options;
	}

	public String print() {
		StringBuilder sb = new StringBuilder();
		line(sb, "===========  QUOTE  begin ===========");

		for (Option option : options) {
			line(sb, "- option  [" + option.getCode() + "] ");

			for (Coverage cov : option.getCoverages()) {
				line(sb, cov.print());
			}

			for (Discount discount : option.getDiscounts()) {
				line(sb, discount.print());
			}
		}

		line(sb, "===========  QUOTE  end   ===========");
		return sb.toString();
	}


	private static void line(StringBuilder sb, String str) {
		sb.append(str).append(Formatter.NL);
	}

}
