package pl.decerto.higson.demo.motor.adapter;

import pl.decerto.higson.demo.motor.domain.Vehicle;
import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.Mapping;

public class VehicleAdapter extends Adapter {

	private Vehicle vehicle;

	public VehicleAdapter(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	protected Mapping getMapping() {
		return new Mapping()
			.add("makeId", vehicle.getMakeId())
			.add("make", vehicle.getMake())
			.add("typeId", vehicle.getTypeId())
			.add("modelId", vehicle.getModelId())
			.add("productionYear", vehicle.getProductionYear());
	}

}
