#!/usr/bin/env bash
#
# G11 step 11.6 — the code-shape greps, made re-runnable.
#
# Three of the plan's literal greps match this migration's own explanatory prose or its spec
# files rather than production code. The criteria are sound; their literal expressions are not.
# Each rescope is recorded inline as `RESCOPED:` so the verdict can cite it.
#
# The comment-blanking helper is lifted from `frontend/scripts/g9-style-checks.sh` — same reason:
# G5-G9 legitimately document *why* a construct is absent, using the very token being grepped.
#
# Run from anywhere: bash frontend/scripts/g11-parity-greps.sh
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

failures=0

strip_comments() {
	perl -0777 -pe '
		s{/\*.*?\*/}{ (my $x = $&) =~ s/[^\n]/ /g; $x }gse;
		s{<!--.*?-->}{ (my $x = $&) =~ s/[^\n]/ /g; $x }gse;
		s{^([ \t]*)//.*$}{$1}gm;
	' "$1"
}

# $1 label, $2 pattern, $3 find-expression for files (eval'd), comments blanked before matching.
absent() {
	local label="$1" pattern="$2" files="$3"
	local hits="" file match
	while IFS= read -r file; do
		[ -z "$file" ] && continue
		match="$(strip_comments "$file" | grep -nE "$pattern" | sed "s|^|$file:|")"
		[ -n "$match" ] && hits+="$match"$'\n'
	done < <(eval "$files")
	if [ -n "$hits" ]; then
		printf 'FAIL  %s\n%s' "$label" "$hits"
		failures=$((failures + 1))
	else
		printf 'ok    %s\n' "$label"
	fi
}

# $1 label, $2 pattern, $3 find-expression, $4 expected count
exactly() {
	local label="$1" pattern="$2" files="$3" want="$4"
	local n=0 file
	while IFS= read -r file; do
		[ -z "$file" ] && continue
		n=$((n + $(strip_comments "$file" | grep -cE "$pattern")))
	done < <(eval "$files")
	if [ "$n" -eq "$want" ]; then
		printf 'ok    %s (%d)\n' "$label" "$n"
	else
		printf 'FAIL  %s — expected %d, found %d\n' "$label" "$want" "$n"
		failures=$((failures + 1))
	fi
}

ALL_SRC="find frontend/src -type f \\( -name '*.ts' -o -name '*.html' -o -name '*.less' -o -name '*.scss' \\) | sort"
# RESCOPED (AC-C5): production TypeScript only. `provideHttpClient` / `HttpTestingController` are
# legitimate in *.spec.ts — the criterion is that no COMPONENT injects HttpClient.
PROD_TS="find frontend/src -type f -name '*.ts' ! -name '*.spec.ts' | sort"
PROD_COMPONENT_TS="find frontend/src/app/components -type f -name '*.ts' ! -name '*.spec.ts' | sort"
PROD_ALL="find frontend/src -type f \\( -name '*.ts' -o -name '*.html' \\) ! -name '*.spec.ts' | sort"

echo "--- AC-S1..AC-S8, AC-C2..AC-C6 (plan step 11.6) ---"
absent  'AC-S1  no `standalone: true` (it is the v20+ default)' 'standalone: true'                  "$ALL_SRC"
absent  'AC-S2  no @Input/@Output decorators'                   '@Input|@Output'                     "$ALL_SRC"
absent  'AC-S2  no *ngIf/*ngFor/ngClass/ngStyle'                '\*ngIf|\*ngFor|ngClass|ngStyle'     "$ALL_SRC"
absent  'AC-S3  no BehaviorSubject (signals only)'              'BehaviorSubject'                    "$ALL_SRC"
absent  'AC-S4  no `: any`'                                     ': any'                              "$ALL_SRC"
# RESCOPED (AC-S5): comments blanked — coverage.spec.ts:12 documents *why* no LOCALE_ID is provided.
absent  'AC-S5/DR-11  no LOCALE_ID / registerLocaleData'        'LOCALE_ID|registerLocaleData'       "$ALL_SRC"
absent  'AC-S6/DR-20  no <base> element'                        '<base'                              "echo frontend/src/index.html"
exactly 'AC-S8/C-8  bypassSecurityTrustHtml appears exactly once' 'bypassSecurityTrustHtml'          "$PROD_TS" 1
absent  'AC-C2  no JSON.stringify (corrupts string PUT bodies)' 'JSON\.stringify'                    "$ALL_SRC"
absent  'AC-C4  no leading-slash HTTP URLs'                     "http\.(get|put)\('/"                "$ALL_SRC"
# RESCOPED (AC-C5): production component TypeScript only — see PROD_COMPONENT_TS above.
absent  'AC-C5  no component injects HttpClient'                'HttpClient'                         "$PROD_COMPONENT_TS"
# RESCOPED (AC-C6): a trailing hyphen is excluded, so `[attr.data-testid]` does not read as an
# unwrap. The criterion is about `HttpClient` returning the body directly where `$http` returned an
# envelope; a template binding that happens to spell `.data-` is not that.
absent  'AC-C6  no leftover `response.data` unwraps'            '\.data([^-]|$)'                     "$ALL_SRC"

echo
echo "--- AC-S7: the documented single-Sass-file exception ---"
scss="$(find frontend/src -name '*.scss' | sort)"
if [ "$scss" = "frontend/src/theme.scss" ]; then
	printf 'ok    AC-S7  the only .scss is frontend/src/theme.scss\n'
else
	printf 'FAIL  AC-S7  unexpected .scss files:\n%s\n' "$scss"
	failures=$((failures + 1))
fi

echo
echo "--- defect-register greps from the traceability matrix ---"
absent  'DR-05  no client-side sort (the backend already sorts)' '\.sort\(|orderBy'                  "$PROD_ALL"
# RESCOPED (DR-16): production only — sidebar.spec.ts names `toggleSidebar` in the assertion
# message of the very test that proves it was not ported.
absent  'DR-16  no sidebar collapse state'                       'sidebarHidden|toggleSidebar'       "$PROD_ALL"
absent  'DR-17  no `selectedDate` dual-scope binding'            'selectedDate'                      "$ALL_SRC"
absent  'DR-18  no `priceBeforeDiscount` (singular) typo'        'priceBeforeDiscount\b'             "$ALL_SRC"
absent  'DR-19  no datepicker close-text'                        'close-text|closeText'              "$ALL_SRC"
absent  'ND-4   no form validation was introduced'               'Validators|mat-error|required='    "$ALL_SRC"
# RESCOPED (ND-7): production only — app.spec.ts must query for `router-outlet` in order to assert
# that zero of them are rendered, and names `provideRouter` in an assertion message.
absent  'ND-7   no router was introduced'                        'provideRouter|router-outlet'       "$PROD_ALL"

echo
echo "--- DR-20: the shell selector ---"
if grep -q '<app-root' frontend/src/index.html; then
	printf 'ok    DR-20  index.html hosts <app-root>\n'
else
	printf 'FAIL  DR-20  index.html has no <app-root>\n'; failures=$((failures + 1))
fi

echo
echo "--- G6 step 6.6, restated so it is satisfiable (see verdict) ---"
exactly 'DR-13  vehicle component routes changes through vehicleFieldChanged()' 'vehicleFieldChanged' \
	"find frontend/src/app/components/vehicle -name '*.ts' -o -name '*.html' | sort" 4
exactly 'DR-13  vehicle component never calls recalculate() directly' '\.recalculate\(' \
	"find frontend/src/app/components/vehicle -name '*.ts' ! -name '*.spec.ts' | sort" 0

echo
if [ "$failures" -eq 0 ]; then
	printf 'ALL CHECKS PASSED\n'
else
	printf '%d CHECK(S) FAILED\n' "$failures"
fi
exit "$failures"
