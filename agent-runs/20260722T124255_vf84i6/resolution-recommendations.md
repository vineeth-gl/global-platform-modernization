---
agent: resolution-recommendation-agent
cli: Cursor Agent CLI
llm: default (CLI-selected)
run_id: 20260722T124255_vf84i6
generated_at: 2026-07-22T07:13:12.136Z
---

### Resolution Recommendations

**Incident ID:** MAD-115
**Based on:** RCA Report + Knowledge Retrieval + local OAuth shallow clone of `vineeth-gl/global-platform-modernization`

#### Option 1: Code Fix (Recommended)

**Risk:** Low
**Effort:** Minutes
**Confidence:** High

**Description:** Remove the `X-Legacy-Bypass: 1` early-return authentication shortcuts from monolith route authorization and the shared OAuth filter. Add a regression test proving the bypass header alone is rejected while valid scoped authentication still succeeds.

**Code Change:**
```java
// Before
if ("1".equals(req.headers("X-Legacy-Bypass"))) {
    return;
}
Map<String, Object> info = FED.introspect(req.headers("Authorization"));

// After
Map<String, Object> info = FED.introspect(req.headers("Authorization"));
```

```java
// Before
if ("1".equals(request.headers("X-Legacy-Bypass"))) {
    return;
}
Map<String, Object> info = fed.introspect(request.headers("Authorization"));

// After
Map<String, Object> info = fed.introspect(request.headers("Authorization"));
```

**Why this works:** Protected endpoints now always validate the Authorization token and required scopes. The legacy header no longer satisfies authentication or authorization for order, customer, admin, or shared OAuth-filtered routes.

**Files modified:**
- `apps/monolith-core/src/main/java/com/demb/monolith/MonolithApp.java`
- `apps/monolith-core/src/main/java/com/demb/monolith/auth/IdentityFederation.java`
- `apps/monolith-core/pom.xml`
- `apps/monolith-core/src/test/java/com/demb/monolith/auth/IdentityFederationTest.java`

#### Option 2: Rollback / Revert

**Risk:** Medium
**Effort:** Minutes
**Side effects:** Reverting the introducing commit would remove any other unrelated changes in that commit. The exact introducing commit was not identified in this shallow clone context.

**Command:**
```bash
git log -S 'X-Legacy-Bypass' -- apps/monolith-core/src/main/java/com/demb/monolith apps/monolith-core/src/main/java/com/demb/monolith/auth
git revert <introducing-commit-hash>
```

**When to use:** Use only if the bypass was introduced by a recent isolated commit and a full code fix cannot be safely deployed inside the P1 window.

#### Option 3: Configuration / Edge Containment

**Risk:** Medium
**Effort:** Minutes to hours

**Change:**
- File: API gateway / WAF / ingress config outside this clone
- Current: Requests with `X-Legacy-Bypass: 1` can reach protected monolith routes
- Correct: Strip or reject `X-Legacy-Bypass` at the perimeter and alert on attempted use

**Why this is partial:** Perimeter blocking reduces exposure, but the application bug remains. It should be used only as immediate containment while Option 1 is deployed.

#### Risk Analysis

| Option | Risk | Speed | Completeness | Recommended? |
|--------|------|-------|--------------|--------------|
| Code Fix | Low | Minutes | Full for known root cause | Yes |
| Rollback | Medium | Minutes | Depends on introducing commit isolation | No |
| Config Fix | Medium | Minutes to hours | Partial containment | No |

#### Recommended Action

Option 1 is recommended and has been applied to the local OAuth clone on branch `feature/MAD-115-20260722T124255_vf84i6` for bridge commit/push. The fix directly removes the broken access-control bypass in both confirmed auth code paths and adds regression coverage.

#### Validation

- `mvn -pl apps/monolith-core test` passed.
- Test result: 2 tests run, 0 failures, 0 errors, 0 skipped.

#### Prevention

- Keep regression coverage for bypass-header-only requests returning 401.
- Add endpoint/integration coverage for `/api/v1/orders` and `/api/v1/admin/flags` without Authorization but with `X-Legacy-Bypass: 1`.
- Add gateway/WAF monitoring for any inbound `X-Legacy-Bypass` header usage.
- Remove or document remaining internal callers that send this header so future services do not rely on it.

#### Bridge Handoff

Branch name: `feature/MAD-115-20260722T124255_vf84i6`
Files pushed by bridge: source/test changes listed above after this agent completes.
PR note: PR Agent / Fix Deployment should open the pull request; this step does not create a PR.
