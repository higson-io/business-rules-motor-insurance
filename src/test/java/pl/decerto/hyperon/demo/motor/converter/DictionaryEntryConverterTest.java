package pl.decerto.hyperon.demo.motor.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.decerto.hyperon.demo.motor.api.dto.DictionaryEntryDto;
import pl.decerto.hyperon.demo.motor.service.DictionaryEntry;

public class DictionaryEntryConverterTest {

	private DictionaryEntryConverter dictionaryEntryConverter;

	@Before
	public void setUp() throws Exception {
		dictionaryEntryConverter = new DictionaryEntryConverter();
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenEntryIsNull() throws Exception {
		dictionaryEntryConverter.convert(null);
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenEntryListIsNull() throws Exception {
		dictionaryEntryConverter.convertList(null);
	}

	@Test
	public void shouldConvertDictionaryEntry() throws Exception {
		//given
		DictionaryEntry dictionaryEntry = createDictionaryEntry();
		//when
		DictionaryEntryDto result = dictionaryEntryConverter.convert(dictionaryEntry);

		//then
		Assert.assertNotNull(result);
		Assert.assertEquals(result.getName(), dictionaryEntry.getName());
		Assert.assertEquals(result.getCode(), dictionaryEntry.getCode());
	}

	@Test
	public void shouldConvertDictionaryEntryList() throws Exception {
		//given
		DictionaryEntry dictionaryEntry = createDictionaryEntry();
		List<DictionaryEntry> dictionaryEntries = new ArrayList<>();
		dictionaryEntries.add(dictionaryEntry);
		//when
		List<DictionaryEntryDto> result = dictionaryEntryConverter.convertList(dictionaryEntries);

		//then
		Assert.assertTrue(CollectionUtils.isNotEmpty(result));
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(result.get(0).getName(), dictionaryEntry.getName());
		Assert.assertEquals(result.get(0).getCode(), dictionaryEntry.getCode());
	}

	private DictionaryEntry createDictionaryEntry() {
		String name = UUID.randomUUID().toString();
		String code = UUID.randomUUID().toString();
		DictionaryEntry dictionaryEntry = new DictionaryEntry();
		dictionaryEntry.setName(name);
		dictionaryEntry.setCode(code);
		return dictionaryEntry;
	}
}