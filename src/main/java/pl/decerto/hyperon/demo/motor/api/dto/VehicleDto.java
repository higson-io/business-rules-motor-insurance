package pl.decerto.hyperon.demo.motor.api.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class VehicleDto {
	private int makeId;
	private String make;
	private int modelId;
	private int typeId;
	private int productionYear;

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public void setMakeId(int makeId) {
		this.makeId = makeId;
	}

	public int getMakeId() {
		return makeId;
	}

	public void setModelId(int modelId) {
		this.modelId = modelId;
	}

	public int getModelId() {
		return modelId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setProductionYear(int productionYear) {
		this.productionYear = productionYear;
	}

	public int getProductionYear() {
		return productionYear;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("makeId", makeId)
				.append("make", make)
				.append("modelId", modelId)
				.append("typeId", typeId)
				.append("productionYear", productionYear)
				.toString();
	}
}
