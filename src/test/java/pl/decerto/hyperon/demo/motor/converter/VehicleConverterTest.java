package pl.decerto.hyperon.demo.motor.converter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.decerto.hyperon.demo.motor.api.dto.VehicleDto;
import pl.decerto.hyperon.demo.motor.domain.Vehicle;

public class VehicleConverterTest {

	private VehicleConverter vehicleConverter;

	@Before
	public void setUp() throws Exception {
		vehicleConverter = new VehicleConverter();
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenVehicleIsNull() throws Exception {
		vehicleConverter.convert(null);
	}

	@Test
	public void shouldConvertVehicle() throws Exception {
		Vehicle vehicle = createVehicle();

		//when
		VehicleDto result = vehicleConverter.convert(vehicle);

		//then
		Assert.assertNotNull(result);
		Assert.assertEquals(vehicle.getMakeId(), result.getMakeId());
		Assert.assertEquals(vehicle.getTypeId(), result.getTypeId());
		Assert.assertEquals(vehicle.getModelId(), result.getModelId());
		Assert.assertEquals(vehicle.getProductionYear(), result.getProductionYear());
	}

	private Vehicle createVehicle() {
		Vehicle vehicle = new Vehicle();
		vehicle.setModelId(123);
		vehicle.setTypeId(213);
		vehicle.setMakeId(321);
		vehicle.setProductionYear(1990);
		return vehicle;
	}
}