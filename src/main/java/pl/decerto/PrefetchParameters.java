package pl.decerto;

public enum PrefetchParameters {
	MAKE_DICTIONARY("demo.motor.dict.vehicle.availableMakes"),
	MODEL_DICTIONARY("demo.motor.dict.vehicle.availableModels"),
	TYPE_DICTIONARY("demo.motor.dict.vehicle.availableTypes");
	private final String code;

	PrefetchParameters(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
