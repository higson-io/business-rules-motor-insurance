import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Group } from './group';
import { LifeQuoteDto } from '../../../models/life';
import { LIFE_QUOTE } from '../../../testing/shell-harness';

/**
 * Granica między fazą oferty a fazą programu (D-2) — jedyne miejsce w tym demie, w którym o tym,
 * co ekran pokazuje, decyduje kształt danych, a nie przełącznik.
 *
 * Dwie rzeczy mogą cicho przestać być prawdą i te testy je łapią.
 *
 * Pierwsza: komponent wyliczający fazę z czegokolwiek innego niż `enrolment` z serwera. Taki ekran
 * działałby dopóty, dopóki reguła i jego własne kryterium się zgadzają.
 *
 * Druga: `programCost` renderowany jako zero w fazie oferty. Reguła zwraca tam `null`, bo bez
 * rozkładu wyborów rzeczywisty koszt nie istnieje — a zero jest twierdzeniem, że program nic nie
 * kosztuje, i to twierdzeniem fałszywym.
 */

const VARIANTS = [
	{ code: 'BASIC', name: 'Podstawowy', position: 1, available: true, perEmployee: 40.66, monthlyTotalIfAll: 2439.6 },
	{ code: 'COMPLETE', name: 'Pełny', position: 3, available: true, perEmployee: 91.01, monthlyTotalIfAll: 5460.6 },
];

function quote(overrides: Partial<LifeQuoteDto>): LifeQuoteDto {
	return {
		...LIFE_QUOTE,
		lineCode: 'GROUP',
		headcount: 100,
		participation: 60,
		variants: VARIANTS,
		benefits: [],
		...overrides,
	};
}

describe('Group — faza oferty a faza programu', () => {
	let httpTesting: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideZonelessChangeDetection(),
				provideNoopAnimations(),
				provideHttpClient(),
				provideHttpClientTesting(),
			],
		});
		httpTesting = TestBed.inject(HttpTestingController);
	});

	async function render(first: LifeQuoteDto): Promise<{
		element: HTMLElement;
		component: Group;
	}> {
		const fixture = TestBed.createComponent(Group);
		TestBed.tick();
		httpTesting.expectOne('life').flush(first);
		await TestBed.inject(ApplicationRef).whenStable();
		fixture.detectChanges();
		return { element: fixture.nativeElement as HTMLElement, component: fixture.componentInstance };
	}

	it('w fazie oferty nie pokazuje kosztu programu ani skąd wzięła się liczba ubezpieczonych', async () => {
		const { element } = await render(
			quote({ enrolment: {}, insuredCount: 60, programCost: null }),
		);

		expect(
			element.querySelector('[data-testid="group-program"]'),
			'bez rozkładu wyborów koszt programu nie istnieje — nie ma go też na ekranie',
		).toBeNull();
		expect(element.querySelector('[data-testid="group-insured"]')?.textContent).toContain('60');
		expect(element.textContent).not.toContain('z przystąpień');
		expect(element.querySelector('[data-testid="group-clear-enrolment"]')).toBeNull();
	});

	it('fazę czyta z rozkładu, który przysłał serwer, a nie z własnego kryterium', async () => {
		// ⚠️ Rozkład NIEZGODNY z insuredCount i z participation: gdyby komponent wyprowadzał fazę
		// z liczby ubezpieczonych albo z partycypacji, ten test przeszedłby dla złego powodu.
		const { element } = await render(
			quote({
				enrolment: { BASIC: 30, COMPLETE: 15 },
				insuredCount: 45,
				participation: 60,
				programCost: 2584.95,
			}),
		);

		const program = element.querySelector('[data-testid="group-program"]');
		expect(program?.textContent).toContain('2584,95');
		expect(element.querySelector('[data-testid="group-insured"]')?.textContent).toContain(
			'z przystąpień',
		);
		expect(
			(element.querySelector('[data-testid="group-enrolment-BASIC"]') as HTMLInputElement).value,
		).toBe('30');
	});

	it('czyści przystąpienia PO KOLEI, bo agregat sesyjny gubi równoległe zapisy', async () => {
		const { component } = await render(
			quote({ enrolment: { BASIC: 30, COMPLETE: 15 }, insuredCount: 45, programCost: 2584.95 }),
		);

		component.clearEnrolment();
		TestBed.tick();

		// ⛔ Sedno testu: po pierwszym tick-u leci DOKŁADNIE JEDNO żądanie. Wersja z Promise.all
		// wysyłała oba naraz i na działającym backendzie czyściła jeden wariant z dwóch —
		// `@SessionScope` nie jest bezpieczny wątkowo, a lista przystąpień to zwykła ArrayList.
		const first = httpTesting.expectOne('life/group/enrolment/BASIC');
		expect(
			first.request.body,
			'zero USUWA pozycję po stronie serwera — to jedyna droga powrotu do fazy oferty',
		).toBe(0);
		httpTesting.expectNone('life/group/enrolment/COMPLETE');

		first.flush(null);
		await Promise.resolve();
		await Promise.resolve();
		TestBed.tick();

		const second = httpTesting.expectOne('life/group/enrolment/COMPLETE');
		expect(second.request.body).toBe(0);
	});
});
