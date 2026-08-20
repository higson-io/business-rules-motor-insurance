package pl.decerto.higson.demo.life.adapter;

import io.higson.runtime.ext.adapter.Adapter;
import io.higson.runtime.ext.adapter.CollectionAdapter;
import io.higson.runtime.ext.adapter.Mapping;
import pl.decerto.higson.demo.life.domain.Enrolment;
import pl.decerto.higson.demo.life.domain.GroupScheme;

/**
 * Wystawia program grupowy pod ścieżką quote.group.*
 *
 * Nie ma tu żadnego odwołania do quote.insured — rozdzielenie kontekstów jest tym, co sprawia,
 * że tabela grupowa czytająca BMI jest niemożliwa, a nie tylko niezalecana.
 */
public class GroupSchemeAdapter extends Adapter {

	private final GroupScheme group;

	public GroupSchemeAdapter(GroupScheme group) {
		this.group = group;
	}

	@Override
	protected Mapping getMapping() {
		if (group == null) {
			return new Mapping();
		}
		return new Mapping()
			.add("headcount", group.getHeadcount())
			.add("averageAge", group.getAverageAge())
			.add("industryClass", group.getIndustryClass())
			.add("participation", group.getParticipation())
			.add("includeSpouse", group.isIncludeSpouse())
			.add("includeAdultChild", group.isIncludeAdultChild())
			.add("enrolment", new CollectionAdapter(group.getEnrolment(),
				entry -> new EnrolmentAdapter((Enrolment) entry)));
	}
}
