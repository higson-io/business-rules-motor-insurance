## Coding Style

### Naming Consistency
Follow established naming patterns for variables, functions, classes, and files throughout the project.

### Automatic Formatting
Use automated tools to enforce consistent indentation, spacing, and line breaks.

### Descriptive Names
Choose names that clearly communicate intent; avoid cryptic abbreviations or single-letter identifiers outside tight loops.

### Focused Functions
Write functions that do one thing well; smaller functions are easier to read, test, and maintain.

### Uniform Indentation
Standardize on spaces or tabs and enforce with editor/linter settings.

### No Dead Code
Remove unused imports, commented-out blocks, and orphaned functions instead of leaving them behind.

### No Backward Compatibility Unless Required
Avoid extra code paths for backward compatibility unless explicitly needed.

### DRY (Don't Repeat Yourself)
Extract repeated logic into reusable functions or modules.

## Project-Specific Coding Style

*The standards below are specific to this project and take precedence over the generic guidance above where they overlap.*

### Tab Indentation Everywhere
All source files — Java, JavaScript, HTML — are indented with hard tabs, not spaces. Continuation lines inside fluent chains get one extra tab. Note there is no `.editorconfig`, no Prettier and no Spotless in this project, so this is maintained by convention alone; configure your editor accordingly.

*Evidence:* 47 of 47 Java files and 13 of 13 JavaScript files are tab-indented.

### UTF-8 Everywhere
Every source and resource root is explicitly configured as UTF-8 in both IDE configurations and in the POM's XML declaration. Never rely on the platform default encoding.

*Evidence:* Eclipse `org.eclipse.core.resources.prefs` sets UTF-8 for src/main/java, src/main/resources and src/test/java; .idea/encodings.xml agrees.

### Keep Java Deliberately Simple
This project is reference material for Higson customers, so readability outranks cleverness. Prefer plain, explicit Java 17 APIs over compact idioms; the demo's job is to make the integration obvious. This is why there are no records, no Lombok, no mapping frameworks and no builders.

*Evidence:* Readme.md: "All the configuration is clear and easy to understand because it is organized in the business domain."
