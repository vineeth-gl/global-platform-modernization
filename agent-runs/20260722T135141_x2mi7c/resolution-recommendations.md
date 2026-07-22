---
agent: resolution-recommendation-agent
cli: Cursor Agent CLI
llm: default (CLI-selected)
run_id: 20260722T135141_x2mi7c
generated_at: 2026-07-22T08:21:52.801Z
---

### Resolution Recommendations

**Incident ID:** MAD-115
**Based on:** RCA Report + Knowledge Retrieval + local source inspection

#### Option 1: Code Fix (Recommended)

**Risk:** Low
**Effort:** Minutes
**Confidence:** High

**Description:** Remove the caller-controlled `X-Legacy-Bypass: 1` authentication short-circuit from the monolith auth paths so protected endpoints always require a valid Authorization token and required scopes.

**Code Change:**
```java
// Before (current vulnerable behavior)
private static void require(Request req, Response res, String... scopes) {
    if ("1".equals(req.headers("X-Legacy-Bypass"))) {
        return;
    }
    Map<String, Object> info = FED.introspect(req.headers("Authorization"));
    ...
}

// After (fixed behavior)
private static void require(Request req, Response res, String... scopes) {
    Map<String, Object> info = FED.introspect(req.headers("Authorization"));
    if (!Boolean.TRUE.equals(info.get("active"))) {
        halt(401, "{\"error\":\"unauthorized\",\"hint\":\"use Bearer dev-admin\"}");
    }
    ...
}
```

```java
// Before (current vulnerable filter behavior)
public void handle(Request request, Response response) {
    if ("1".equals(request.headers("X-Legacy-Bypass"))) {
        return;
    }
    Map<String, Object> info = fed.introspect(request.headers("Authorization"));
    ...
}

// After (fixed filter behavior)
public void handle(Request request, Response response) {
    Map<String, Object> info = fed.introspect(request.headers("Authorization"));
    if (!Boolean.TRUE.equals(info.get("active"))) {
        halt(401, "{\"error\":\"unauthorized\",\"hint\":\"use Bearer dev-admin\"}");
    }
    ...
}
```

**Why this works:** The exploit depends on returning before token introspection and scope checks. Removing those early returns makes `X-Legacy-Bypass` inert for protected routes: requests without valid auth now fail with 401, and valid users still require the needed scope or admin privilege.

**Files modified locally:**
- `apps/monolith-core/src/main/java/com/demb/monolith/MonolithApp.java`
- `apps/monolith-core/src/main/java/com/demb/monolith/auth/IdentityFederation.java`

**Validation performed:** `mvn -pl apps/monolith-core -am test` completed successfully. The module currently has no test sources, so Maven reported no tests to run.

#### Option 2: Rollback

**Risk:** Medium
**Effort:** Minutes
**Side effects:** Rollback is only appropriate if a specific commit introducing the bypass can be confirmed. Local prompt/history did not provide the introducing commit, and the current workspace should not blindly revert unrelated monolith changes.

**Command:**
```bash
git log -- apps/monolith-core/src/main/java/com/demb/monolith/MonolithApp.java apps/monolith-core/src/main/java/com/demb/monolith/auth/IdentityFederation.java
git revert <introducing-commit-hash>
```

**When to use:** Use rollback if incident containment must happen immediately and `git log` confirms a narrow commit that only added the bypass behavior without reverting unrelated shipped fixes.

#### Option 3: Defense-in-Depth Header Cleanup

**Risk:** Medium
**Effort:** Hours

**Change:** Remove or replace internal callers that still send `X-Legacy-Bypass`, then strip the header at ingress/service boundary.

**Current references to review:**
- `apps/monolith-core/src/main/java/com/demb/monolith/HttpClient.java`
- `apps/svc-billing/src/main/java/com/demb/billing/BillingApp.java`
- `apps/svc-inventory/server.js`
- `security/zero_trust_policy.yaml`
- `security/OWASP_BACKLOG.md`

**Correct state:** Internal service calls should authenticate with supported service credentials, and ingress should drop `X-Legacy-Bypass` from external traffic. This is complementary to Option 1, not a substitute for fail-closed application auth.

#### Risk Analysis

| Option | Risk | Speed | Completeness | Recommended? |
|--------|------|-------|--------------|--------------|
| Code Fix | Low | Minutes | Full for reported bypass | Yes |
| Rollback | Medium | Minutes | Depends on commit scope | No |
| Header Cleanup | Medium | Hours | Defense in depth | As follow-up |

#### Recommended Action

Keep Option 1 as the incident fix because it removes the vulnerable auth bypass at the enforcement point and preserves existing token/scope behavior. Follow with Option 3 to eliminate stale internal bypass usage and ingress exposure.

#### Prevention

- Add regression tests proving `/api/v1/orders` and `/api/v1/admin/flags` reject requests that only include `X-Legacy-Bypass: 1`.
- Add static/security checks for auth middleware early returns controlled by request headers.
- Add ingress/header-stripping policy for legacy bypass headers and alert on any observed inbound `X-Legacy-Bypass` usage.

#### Handoff to QA Validation

- Chosen recommended option: Option 1, code fix.
- Affected files: 2 Java auth enforcement files listed above.
- Risk: Low; protected endpoints should now fail closed unless Authorization is valid and scopes pass.
- Test gap: no automated tests exist in the module yet; add regression coverage before merge if pipeline policy requires tests.

Branch name: `feature/MAD-115-20260722T124255_vf84i6`
Files pushed: bridge-owned push pending for the two modified Java files above; no PR opened by this agent.
Next: PR Agent / Fix Deployment will open the pull request after this step.
