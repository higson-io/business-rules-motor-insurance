package pl.decerto.hyperon.demo.motor.service;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class DictionaryEntry {
	private String code;
	private String name;

	public String getCode() {
		return code;
	}

	public DictionaryEntry setCode(String code) {
		this.code = code;
		return this;
	}

	public String getName() {
		return name;
	}

	public DictionaryEntry setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("code", code)
				.append("name", name)
				.toString();
	}
}
