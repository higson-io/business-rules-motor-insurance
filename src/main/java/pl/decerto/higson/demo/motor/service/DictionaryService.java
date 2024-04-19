package pl.decerto.higson.demo.motor.service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.engine.core.context.ParamContext;
import io.higson.runtime.engine.core.output.MultiValue;
import io.higson.runtime.engine.core.output.ParamValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.higson.runtime.core.HigsonContext;

@Service
public class DictionaryService {

	private final HigsonEngine engine;

	@Autowired
	public DictionaryService(HigsonEngine engine) {
		this.engine = engine;
	}

	public List<DictionaryEntry> getMakeDictionary(int productionYear) {
		return getHigsonDictionary(Dictionaries.VEHICLE_MAKES, new HigsonContext("quote.vehicle.productionYear", productionYear),
				this::vehicleMakeRowToDictionaryEntry);
	}

	public List<DictionaryEntry> getModelDictionary(Integer typeId) {
		return getHigsonDictionary(Dictionaries.VEHICLE_MODEL, new HigsonContext("quote.vehicle.typeId", typeId),
				this::vehicleModelRowToDictionaryEntry);
	}

	public List<DictionaryEntry> getProductionYearDictionary() {
		return getHigsonDictionary(Dictionaries.PRODUCTION_YEAR, new HigsonContext(), this::rowToSimpleEntry);
	}

	public List<DictionaryEntry> getTypeDictionary(Integer makeId) {
		return getHigsonDictionary(Dictionaries.VEHICLE_TYPE, new HigsonContext("quote.vehicle.makeId", makeId),
				this::vehicleTypeRowToDictionaryEntry);
	}

	private <T> List<T> getHigsonDictionary(String parameter, ParamContext context, Function<MultiValue, T> mapFunction) {
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
