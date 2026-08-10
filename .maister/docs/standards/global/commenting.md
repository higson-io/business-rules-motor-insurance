## Commenting

### Let Code Speak
Write code that explains itself through structure and naming.

### Comment Sparingly
Add brief comments only when the logic isn't self-evident from the code.

### No Change Comments
Avoid comments about recent fixes or changes; comments should be timeless explanations, not changelogs.

## Project-Specific Commenting

*The standards below are specific to this project and take precedence over the generic guidance above where they overlap.*

### Explanatory Comments on Non-Obvious Build and Config Entries
Build and configuration files carry short comments recording the intent behind a non-obvious setting: why a property exists, what a plugin does and how to invoke it, why a value is deliberately left unset, and what is dangerous about it. Dependency blocks get grouping labels. This is the highest-signal documentation in the repository — maintain it when you change these files.

```xml
<!-- dialect is auto-detected from the JDBC URL by the Higson runtime starter -->
```

*Evidence:* pom.xml carries commented property groups, the snapshot single-source-of-truth rationale, a token warning, dependency block labels and higson-cli usage instructions; higson-demo-app.properties explains dialect auto-detection, the unset schema, dev-mode and validate-arguments.
