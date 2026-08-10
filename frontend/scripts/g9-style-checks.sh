#!/usr/bin/env bash
#
# G9 check 3 — the static half of the styling group's acceptance criteria.
#
# Every assertion below is a line of the G9 acceptance-criteria list in
# `.maister/tasks/migrations/2026-08-06-angularjs-to-angular/implementation/implementation-plan.md`,
# turned into something that can be re-run. G11 and G12 both need to re-prove these after they
# touch the tree, and a grep that only ever ran once in a transcript proves nothing later.
#
# ⚠️ THE GREPS IGNORE COMMENTS, and that is the whole reason this is a script rather than the bare
# `grep -rn` the plan writes. Nearly every deletion here is documented in the file that replaced
# it — `.coverage__divider` is named in `coverage.less` precisely to record that it was dropped and
# why. A literal grep flags all of that prose and reports a clean tree as dirty, which trains the
# reader to ignore the check. What these assertions are actually about is whether a SELECTOR, an
# IMPORT or a CLASS ATTRIBUTE still exists, so comments are blanked out before matching.
#
# Run from anywhere: bash frontend/scripts/g9-style-checks.sh
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

failures=0

# Blanks out `/* … */`, `<!-- … -->` and `// …` while preserving line numbering, so a hit still
# points at the right line. Trailing `//` comments after code are left alone on purpose: blanking
# those would also eat the `//` of a `url('https://…')` and make the CDN check pass for the wrong
# reason.
strip_comments() {
	perl -0777 -pe '
		s{/\*.*?\*/}{ (my $x = $&) =~ s/[^\n]/ /g; $x }gse;
		s{<!--.*?-->}{ (my $x = $&) =~ s/[^\n]/ /g; $x }gse;
		s{^([ \t]*)//.*$}{$1}gm;
	' "$1"
}

source_files() {
	find "$@" -type f \
		\( -name '*.less' -o -name '*.scss' -o -name '*.ts' -o -name '*.html' -o -name '*.json' \) \
		2>/dev/null | sort
}

# Asserts that a pattern appears in NO source file, comments excluded.
absent() {
	local label="$1" pattern="$2"
	shift 2
	local hits="" file match
	while IFS= read -r file; do
		match="$(strip_comments "$file" | grep -nE "$pattern" | sed "s|^|$file:|")"
		[ -n "$match" ] && hits+="$match"$'\n'
	done < <(source_files "$@")
	if [ -n "$hits" ]; then
		printf 'FAIL  %s\n%s' "$label" "$hits"
		failures=$((failures + 1))
	else
		printf 'ok    %s\n' "$label"
	fi
}

# Asserts that a pattern DOES appear — used for the two things G9 must preserve rather than remove.
present() {
	local label="$1" pattern="$2" file="$3"
	if strip_comments "$file" | grep -qE "$pattern"; then
		printf 'ok    %s\n' "$label"
	else
		printf 'FAIL  %s (expected /%s/ in %s)\n' "$label" "$pattern" "$file"
		failures=$((failures + 1))
	fi
}

echo "== G9 static checks =="

# --- DR-25: dead selectors are deleted, not ported -------------------------------------------
absent 'DR-25  no coverage__divider / common-checkbox / common-subsection-* selector' \
	'coverage__divider|common-checkbox|common-subsection-heading|common-subsection-divider' \
	frontend/src

# --- DR-16: the removed sidebar-collapse feature leaves no CSS behind ------------------------
absent 'DR-16  no collapse transition/opacity left in the sidebar styles' \
	'transition|opacity' \
	frontend/src/styles/sidebar.less

# --- DR-24: the two unreferenced legacy binaries are not carried over ------------------------
if find frontend/public frontend/src \( -name 'logo.png' -o -name 'select-arrow.png' \) | grep -q .; then
	echo "FAIL  DR-24  logo.png / select-arrow.png were carried into the new frontend"
	failures=$((failures + 1))
else
	echo "ok    DR-24  logo.png and select-arrow.png are absent from the new frontend"
fi

# --- AC-S7: exactly one Sass file, the Material theme seed ----------------------------------
scss="$(find frontend/src -name '*.scss' | sort | tr '\n' ' ')"
if [ "$scss" = "frontend/src/theme.scss " ]; then
	echo "ok    AC-S7  exactly one .scss file: frontend/src/theme.scss"
else
	echo "FAIL  AC-S7  expected only frontend/src/theme.scss, found: $scss"
	failures=$((failures + 1))
fi

# --- C-9: Bootstrap 3 is gone, as a dependency and as a vocabulary ---------------------------
absent 'C-9    bootstrap is not a dependency' 'bootstrap' frontend/package.json
absent 'C-9    no Bootstrap selector survives in the stylesheets' \
	'\.container[^-_a-zA-Z]|\.row[^-_a-zA-Z]|\.col-md-|\.alert-danger|\.glyphicon' \
	frontend/src/styles
absent 'C-9    no Bootstrap class is applied in a template' \
	'class="[^"]*(col-md-[0-9]|glyphicon|alert-danger)' \
	frontend/src

# --- C-10: the deleted font families leave no reference behind -------------------------------
absent 'C-10   no Font Awesome or Open Sans reference' \
	'font-awesome|FontAwesome|open-sans|npm-font-open-sans' \
	frontend/src frontend/package.json
absent 'C-10   no webfont CDN fetch at build time or run time' \
	'fonts\.googleapis\.com|fonts\.gstatic\.com' \
	frontend/src

# --- 9.4: the collapse point is a media query, not a TypeScript observer ---------------------
absent '9.4    BreakpointObserver is not introduced' 'BreakpointObserver' frontend/src
present '9.4    the 992px collapse point is preserved' '992px' frontend/src/styles/_layout.less

# --- 9.6 / G2: screenshots stay documentation, never an assertion ----------------------------
absent '9.6    toHaveScreenshot appears in no spec' 'toHaveScreenshot' e2e/specs

echo
if [ "$failures" -eq 0 ]; then
	echo "G9 static checks: ALL PASS"
else
	echo "G9 static checks: $failures FAILED"
fi
exit "$failures"
