/**
 * The stage-aware assertion mechanism (implementation plan, step 2.5).
 *
 * Some defect-register rows deliberately change observable behaviour, so a spec that
 * hard-coded today's behaviour would "discover" the sanctioned fix as a failure during
 * the G11 replay. Instead the spec FILES stay byte-identical between capture and replay;
 * only the environment differs, via `MIGRATION_STAGE`.
 *
 * Everything not listed in the step-2.5 expected-delta table MUST be byte-identical pre
 * and post. In particular the two DR-13 / ND-1 request-count assertions in
 * `vehicle-cascade.spec.ts` carry NO branch at all - they are the strongest evidence in
 * the suite that the vehicle emit asymmetry survived the migration.
 */

export type Stage = 'pre' | 'post';

const raw = process.env.MIGRATION_STAGE ?? 'pre';

if (raw !== 'pre' && raw !== 'post') {
	throw new Error(`MIGRATION_STAGE must be 'pre' or 'post', got '${raw}'`);
}

/** `pre` = the AngularJS build being captured; `post` = the Angular build being replayed. */
export const STAGE: Stage = raw;

/**
 * Pick the value expected at the current migration stage.
 *
 * ONLY call this for an assertion backed by a row of the step-2.5 expected-delta table.
 * The `defect` argument is mandatory so every branch names its authority in the code
 * itself, not merely in a comment that can drift away from it.
 */
export function byStage<T>(defect: string, values: { pre: T; post: T }): T {
	if (!/^(DR|ND)-\d{2}$/.test(defect)) {
		throw new Error(
			`byStage() requires a defect-register id (DR-nn / ND-nn) naming the authority for the branch, got '${defect}'`
		);
	}
	return values[STAGE];
}
