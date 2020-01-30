package pl.decerto.hyperon.demo.motor.api.dto;

/**
 * @author Maciej Główka on 30.01.2020
 */
public class UpdateMakeDto {
	private Integer makeId;
	private String make;

	public Integer getMakeId() {
		return makeId;
	}

	public void setMakeId(Integer makeId) {
		this.makeId = makeId;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}
}
