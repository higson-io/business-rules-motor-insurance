package pl.decerto.higson.demo.motor.converter;

import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pl.decerto.higson.demo.motor.api.dto.VehicleDto;
import pl.decerto.higson.demo.motor.domain.Vehicle;

@Component
public class VehicleConverter implements Converter<Vehicle, VehicleDto> {

	@Override
	public VehicleDto convert(Vehicle vehicle) {
		Objects.requireNonNull(vehicle, "Vehicle can not be null.");
		VehicleDto dto = new VehicleDto();
		dto.setMakeId(vehicle.getMakeId());
		dto.setMake(vehicle.getMake());
		dto.setModelId(vehicle.getModelId());
		dto.setTypeId(vehicle.getTypeId());
		dto.setProductionYear(vehicle.getProductionYear());
		return dto;
	}
}
