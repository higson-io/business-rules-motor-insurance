package pl.decerto.hyperon.demo.motor.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Vehicle {

	private int makeId;
	private int typeId;
	private int modelId;
	private int productionYear;

	public int getMakeId() {
		return makeId;
	}

	public void setMakeId(int makeId) {
		this.makeId = makeId;
	}

	public int getModelId() {
		return modelId;
	}

	public void setModelId(int modelId) {
		this.modelId = modelId;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getProductionYear() {
		return productionYear;
	}

	public void setProductionYear(int productionYear) {
		this.productionYear = productionYear;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("makeId", makeId)
				.append("typeId", typeId)
				.append("modelId", modelId)
				.append("productionYear", productionYear)
				.toString();
	}
}
