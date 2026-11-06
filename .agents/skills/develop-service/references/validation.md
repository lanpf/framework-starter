# Baseline Validation and Exception Rules

## Contents

- [Bean Validation](#bean-validation)
- [Explicit checks](#explicit-checks)
- [Exceptions and logging](#exceptions-and-logging)

## Bean Validation

- **JAVA-BEAN-VALIDATION-PREFERENCE-001** **[DEFAULT][BASELINE]** — When dependencies and runtime validation are available, prefer Jakarta Bean Validation for nullability, format, and range constraints on bindable properties and method parameters or returns.
- **JAVA-BEAN-VALIDATION-EXECUTION-001** **[REQUIRED][BASELINE]** — When using Bean Validation, declare constraints at the corresponding field, property, record component, parameter, or return location and ensure the actual call path triggers validation.
- **JAVA-VALIDATION-CASCADE-001** **[REQUIRED][BASELINE]** — `@Valid` marks cascaded validation of nested objects and is not itself a constraint.
- **JAVA-BEAN-VALIDATION-NULLABILITY-001** **[REQUIRED][BASELINE]** — Declare requiredness using each constraint's null semantics. @Valid, @Size, and @Pattern do not reject null and cannot replace a non-null constraint. Combine @NotNull only when the chosen constraint permits null but the contract requires a non-null value.
- **JAVA-BEAN-VALIDATION-CONTAINER-001** **[REQUIRED][BASELINE]** — Declare container constraints, element constraints, and element cascading separately under the contract; none substitutes for another. For example, @NotEmpty List<@NotNull @Valid ItemRequest> constrains container non-emptiness, non-null elements, and element validity respectively.

## Explicit checks

- **JAVA-VALIDATION-002** **[REQUIRED][BASELINE]** — Use explicit checks when Bean Validation cannot express the constraint, the call path cannot guarantee validation, or an explicit business-error meaning is required; this includes constructors and factories outside framework binding that need fast failure.
- **JAVA-EXCEPTION-SEMANTICS-001** **[REQUIRED][BASELINE]** — Classify checks and exceptions by failure semantics: business-contract violations are business failures; programming errors and broken technical invariants are technical failures. Do not classify by code location, variable count, utility choice, or BaseException inheritance.
- **JAVA-VALIDATION-TECHNICAL-001** **[REQUIRED][BASELINE]** — Technical failures must not use business exceptions or codes. Framework code and framework-based services use FrameworkException for technical contracts the framework can identify; use ILLEGAL_ARGUMENT for invalid technical arguments, ILLEGAL_STATE for invalid execution state, and an owning capability error for a stable specific failure. Do not wrap unknown JDK or third-party failures indiscriminately; unified boundaries classify them as INTERNAL_ERROR.
- **JAVA-CHECK-ERROR-MESSAGE-001** **[DEFAULT][BASELINE]** — Prefer explicit diagnostic messages for technical checks. Use default messages only for a concrete reason, such as equivalent context supplied by a common wrapper; message preference never changes exception semantics.
- **JAVA-CHECK-DIAGNOSTIC-CONTENT-001** **[REQUIRED][BASELINE]** — Explicit technical diagnostic messages must be nonblank and identify the checked object and violated constraint, rather than generic text such as invalid or required. Protect sensitive values under the logging rules.
- **JAVA-BUSINESS-CHECK-TOOL-001** **[REQUIRED][BASELINE]** — Choose Require business-exception-supplier overloads or explicit business guards according to the business constraint and readability. Default or string-message overloads must not replace the promised business exception. Availability of a general-purpose check utility does not forbid explicit business guards.
- **JAVA-DEFENSIVE-CHECK-TOOL-001** **[REQUIRED][BASELINE]** — Use framework-kernel Require for ordinary technical argument checks and Require.state for simple technical execution-state checks; do not use Spring Assert, Commons Validate, Objects.requireNonNull, or implicit NPE validation. Use explicit guards for cross-variable conditions or capability-specific FrameworkException errors that Require cannot express clearly.
- **JAVA-VALIDATION-NPE-001** **[REQUIRED][BASELINE]** — Never explicitly throw NPE or rely on implicit NPE for validation; do not use Objects.requireNonNull or Validate.notNull as checks. Reject or normalize foreseeable null input under its contract before dereference or a null-rejecting API. Use Validate.notBlank, notEmpty, or similar null-sensitive methods only after non-nullness is established.
- **JAVA-VALIDATION-003** **[REQUIRED][BASELINE]** — Depend on framework-kernel directly when using Require or framework exception contracts. Do not add Spring, Commons Lang, or arbitrary utility libraries solely for checks. Do not hand-write general checks already satisfied by Require; explicit business and cross-variable guards remain allowed.

## Exceptions and logging

- **JAVA-NPE-DEFECT-001** **[REQUIRED][BASELINE]** — Treat an unexpected NPE as a programming defect to diagnose and fix; never catch it uniformly and disguise it as an expected business rejection. A unified exception boundary may record diagnostics and return a generic system failure without changing its defect classification.
- **JAVA-EXCEPTION-LANGUAGE-001** **[REQUIRED][BASELINE]** — Default messages for common framework technical errors such as FrameworkError, project-authored technical diagnostics, and Bean Validation messages use English. Original third-party messages are exempt from this language requirement. Business-template language and stability follow the error-code policy.
- **JAVA-EXCEPTION-PROTOCOL-001** **[REQUIRED][BASELINE]** — Determine business outcomes by stable business error codes; never parse an exception message as a stable protocol.
- **JAVA-EXCEPTION-003** **[REQUIRED][BASELINE]** — Handle, translate, or propagate exceptions under their contracts; never silently swallow them. Follow the logging rules when recording exceptions.
