---
name: manage-service-error-codes
description: Use when allocating, adding, modifying, documenting, or reviewing application, domain, aggregate, or infrastructure error codes in a service.
---

# Manage Service Error Codes

## Workflow

1. Read `references/error-codes.md` completely.
2. Read the service's project documentation entry, follow its routing to the authoritative domain document, and inspect every existing error enum before allocating a code.
3. Confirm the service prefix, layer range, aggregate range, name prefix, and next available local code.
4. Append within the owning range; never modify, reuse, or reassign a published code.
5. Update the authoritative domain document and tests in the same change.
6. Report conflicts instead of choosing a new range without authorization.
