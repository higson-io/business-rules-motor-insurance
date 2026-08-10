import {
	ApplicationConfig,
	provideBrowserGlobalErrorListeners,
	provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';

import { IsoDateAdapter, MOTOR_DATE_FORMATS } from './core/iso-date.adapter';
import { mutationErrorInterceptor } from './core/mutation-error.interceptor';
import { pendingRequestsInterceptor } from './core/pending-requests.interceptor';

// Deliberately no locale provider and no Polish locale registration. Angular's default en-US
// locale data is exactly what the current `| number: 2` output already produces
// (100000 -> 100,000.00) despite the page being served with lang="pl". Registering a Polish
// locale here would silently change every formatted amount in the application (DR-11).
export const appConfig: ApplicationConfig = {
	providers: [
		provideZonelessChangeDetection(),
		provideBrowserGlobalErrorListeners(),
		provideHttpClient(withInterceptors([pendingRequestsInterceptor, mutationErrorInterceptor])),
		provideAnimationsAsync(),
		{ provide: DateAdapter, useClass: IsoDateAdapter },
		{ provide: MAT_DATE_FORMATS, useValue: MOTOR_DATE_FORMATS },
	],
};
