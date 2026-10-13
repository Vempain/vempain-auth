---
name: change-acl-authorization
description: Change the ACL evaluation rules of vempain-auth (AclAuthorizationService, unit membership, fail-closed behaviour) with the full test coverage the library requires. Use for any authorization or unit membership change.
---

# Change ACL authorization in vempain-auth

- `AclAuthorizationService` (core) grants an operation only when the user or one of their effective units has an ACL row with the requested
  read/create/modify/delete privilege for the resource's `acl_id`. It fails closed: non-positive `acl_id`, missing rows, anonymous or
  non-Vempain principals yield `false`. Keep it role free: no `hasRole`/`ROLE_*`, no test-mode bypass.
- Units nest (`unit_unit`, `UnitMembershipService`); `UserDetailsServiceImpl.loadUserByUsername` builds the principal with the effective
  units (direct plus every containing unit). `UnitService.updateMembers` rejects cycles with 400 (`wouldCreateCycle`).
- Coverage rule: `AclAuthorizationService` stays at 100% line coverage in this repository. Extend all three layers with granted and denied
  cases: `AclAuthorizationServiceUTC` (every branch, mocks), `AclAuthorizationServiceITC` (real ACL rows in PostgreSQL),
  `test-service` `TestServiceSecurityITC` (`@PreAuthorize("@aclAuthorizationService.canRead(#p0)")` over MockMvc).
- Unit membership changes: `UnitMembershipServiceUTC`, `UnitMembershipITC`, `UserUnitManagementCTC`, `UserDetailsServiceImplUTC`,
  `UnitMembershipContractJTC`.
- `IntegrationTestSetup` keeps the seeded administrator (`Constants.ADMIN_ID == 1L`); never delete all users in a test. Hash test
  passwords with `AuthTools.passwordHash` or the configured encoder.
- Consumers rely on the semantics (`AccessService` in the admin backend, `FileAclService` in the file backend); describe behavioural
  changes in `AGENTS.md` and release with a version bump.
