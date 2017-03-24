package pl.decerto.hyperon.demo.motor.service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.smartparam.engine.core.context.ParamContext;
import org.smartparam.engine.core.output.MultiValue;
import org.smartparam.engine.core.output.ParamValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.decerto.hyperon.runtime.core.HyperonContext;
import pl.decerto.hyperon.runtime.core.HyperonEngine;

@Service
public class DictionaryService {

	private final HyperonEngine engine;

	@Autowired
	public DictionaryService(HyperonEngine engine) {
		this.engine = engine;
	}

	public List<DictionaryEntry> getMakeDictionary(int productionYear) {
		return getHyperonDictionary(Dictionaries.VEHICLE_MAKES, new HyperonContext("quote.vehicle.productionYear", productionYear),
				this::vehicleMakeRowToDictionaryEntry);
	}

	public List<DictionaryEntry> getModelDictionary(Integer typeId) {
		return getHyperonDictionary(Dictionaries.VEHICLE_MODEL, new HyperonContext("quote.vehicle.typeId", typeId),
				this::vehicleModelRowToDictionaryEntry);
	}

	public List<DictionaryEntry> getProductionYearDictionary() {
		return getHyperonDictionary(Dictionaries.PRODUCTION_YEAR, new HyperonContext(), this::rowToSimpleEntry);
	}

	public List<DictionaryEntry> getTypeDictionary(Integer makeId) {
		return getHyperonDictionary(Dictionaries.VEHICLE_TYPE, new HyperonContext("quote.vehicle.makeId", makeId),
				this::vehicleTypeRowToDictionaryEntry);
	}

	private <T> List<T> getHyperonDictionary(String parameter, ParamContext context, Function<MultiValue, T> mapFunction) {
		ParamValue paramValue = engine.get(parameter, context);
		return getResultFromParamValue(paramValue, mapFunction);
	}

	private <T> List<T> getResultFromParamValue(ParamValue paramValue, Function<MultiValue, T> mapFunction) {
		return paramValue.rows().stream()
				.map(mapFunction)
				.collect(Collectors.toList());
	}

	private DictionaryEntry vehicleMakeRowToDictionaryEntry(MultiValue r) {
		return new DictionaryEntry()
				.setCode(r.getString("make_id"))
				.setName(r.getString("make"));
	}

	private DictionaryEntry vehicleModelRowToDictionaryEntry(MultiValue r) {
		return new DictionaryEntry()
				.setCode(r.getString("model_id"))
				.setName(r.getString("model_label"));
	}

	private DictionaryEntry vehicleTypeRowToDictionaryEntry(MultiValue r) {
		return new DictionaryEntry()
				.setCode(r.getString("type_id"))
				.setName(r.getString("type"));
	}

	private DictionaryEntry rowToSimpleEntry(MultiValue r) {
		return new DictionaryEntry()
				.setCode(r.getString("code"))
				.setName(r.getString("name"));
	}
}
