package pl.decerto.higson.demo.motor.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.decerto.higson.demo.motor.api.dto.UpdateMakeDto;
import pl.decerto.higson.demo.motor.api.dto.VehicleDto;
import pl.decerto.higson.demo.motor.converter.VehicleConverter;
import pl.decerto.higson.demo.motor.domain.Vehicle;
import pl.decerto.higson.demo.motor.service.VehicleService;

@RestController
@RequestMapping("/vehicle")
public class VehicleApi {

	private final VehicleService vehicleService;
	private final VehicleConverter vehicleConverter;

	@Autowired
	public VehicleApi(VehicleService vehicleService, VehicleConverter vehicleConverter) {
		this.vehicleService = vehicleService;
		this.vehicleConverter = vehicleConverter;
	}

	@GetMapping
	public VehicleDto getVehicle() {
		return vehicleConverter.convert(vehicleService.getVehicle());
	}

	@PutMapping("/productionYear")
	public void setProductionYear(@RequestBody Integer productionYear) {
		Vehicle vehicle = vehicleService.getVehicle();
		vehicle.setProductionYear(productionYear);
		vehicle.setMakeId(0);
		vehicle.setTypeId(0);
		vehicle.setModelId(0);
	}

	@PutMapping("/make")
	public void setMake(@RequestBody UpdateMakeDto makeDto) {
		Vehicle vehicle = vehicleService.getVehicle();
		vehicle.setMake(makeDto.getMake());
		vehicle.setMakeId(makeDto.getMakeId());
		vehicle.setTypeId(0);
		vehicle.setModelId(0);
	}

	@PutMapping("/model")
	public void setModel(@RequestBody Integer modelId) {
		Vehicle vehicle = vehicleService.getVehicle();
		vehicle.setModelId(modelId);
	}

	@PutMapping("/type")
	public void setType(@RequestBody Integer typeId) {
		Vehicle vehicle = vehicleService.getVehicle();
		vehicle.setTypeId(typeId);
		vehicle.setModelId(0);
	}
}
