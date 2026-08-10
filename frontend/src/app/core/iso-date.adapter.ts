import { Injectable } from '@angular/core';
import { MatDateFormats, NativeDateAdapter } from '@angular/material/core';

/**
 * Sentinel for the one format this adapter renders and parses itself. Every other display format
 * stays an `Intl.DateTimeFormat` options object handled by the base adapter, so the calendar header
 * and the accessibility labels keep their normal wording.
 */
const ISO_DATE_FORMAT = 'yyyy-MM-dd';

/** `yyyy-MM-dd`, plus `yyyy/MM/dd` as the alternative the AngularJS datepicker also accepted. */
const ISO_DATE_PATTERN = /^(\d{4})([-/])(\d{1,2})\2(\d{1,2})$/;

/**
 * Date formats for the birth-date picker. The text input round-trips as `yyyy-MM-dd`; the calendar
 * labels are unchanged from the Material defaults.
 */
export const MOTOR_DATE_FORMATS: MatDateFormats = {
	parse: {
		dateInput: ISO_DATE_FORMAT,
	},
	display: {
		dateInput: ISO_DATE_FORMAT,
		monthYearLabel: { year: 'numeric', month: 'short' },
		dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
		monthYearA11yLabel: { year: 'numeric', month: 'long' },
	},
};

/**
 * Renders and parses the birth date as `yyyy-MM-dd` and starts the week on Monday, reproducing the
 * `formatYear: 'yyyy'` / `startingDay: 1` options of the AngularJS datepicker. Week numbers and the
 * button bar are absent by default in Material, so neither needs disabling.
 */
@Injectable()
export class IsoDateAdapter extends NativeDateAdapter {
	override getFirstDayOfWeek(): number {
		return 1;
	}

	override format(date: Date, displayFormat: object | string): string {
		if (displayFormat !== ISO_DATE_FORMAT) {
			return super.format(date, displayFormat);
		}
		const year = this.getYear(date);
		const month = this.getMonth(date) + 1;
		const day = this.getDate(date);
		return `${year}-${pad(month)}-${pad(day)}`;
	}

	override parse(value: unknown): Date | null {
		if (typeof value !== 'string') {
			return super.parse(value);
		}
		const match = ISO_DATE_PATTERN.exec(value.trim());
		if (!match) {
			return super.parse(value);
		}

		const year = Number(match[1]);
		const month = Number(match[3]);
		const day = Number(match[4]);
		const parsed = new Date();
		parsed.setFullYear(year, month - 1, day);
		parsed.setHours(0, 0, 0, 0);

		const roundTrips =
			parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day;
		return roundTrips ? parsed : this.invalid();
	}
}

function pad(value: number): string {
	return String(value).padStart(2, '0');
}
