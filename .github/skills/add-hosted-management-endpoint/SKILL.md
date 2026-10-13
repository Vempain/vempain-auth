---
name: add-hosted-management-endpoint
description: Add or change the user, unit, ACL or login endpoints that vempain-auth implements once and every backend hosts through component scanning. Use when management REST behaviour must change for all backends.
---

# Hosted management endpoints in vempain-auth

- Contracts live in `api/.../rest/{LoginAPI,UserAPI,UnitAPI,AclAPI}.java`, paths from `api/.../Constants.java`; implementations in
  `core/.../controller/{Login,User,Unit,Acl}Controller.java`. Every backend that includes `vempain-auth-core` hosts them
  (`/content-management/users|units|acls`), so never add such an endpoint to a consuming backend.
- `AdministrationGuard` (`core/.../security/`) gates the management endpoints with the modify privilege on the administrator ACL (`Constants.ADMIN_ID`), never a
  role. Controllers only guard and delegate; rules live in `UserService`, `UnitService`, `AclService`,
  `UnitMembershipService`.
- Paging of users and units is in memory (`PagingTools`): name or id sort, search by name/login name.
- DTOs: Lombok builders, snake_case JSON (`tools.jackson` naming), a null membership list in a request leaves that membership untouched.
- Security configuration expects host properties (`vempain.cors.*`, `vempain.app.jwt-secret`, `vempain.app.jwt-expiration-ms`); hosts
  extend `fi.poltsi.vempain.auth.security.WebSecurityConfig` and register extra filters through `additionalAuthenticationFilters`.
- Tests: there is no production application class, so use `core/src/test/java/.../TestApp.java` and `IntegrationTestSetup`; controller
  behaviour in `*CTC` (`LoginCTC`, `UserUnitManagementCTC`), JSON contracts in `*JTC`. Consumers verify the hosting with their
  `HostedManagement*` tests, and `vempain-auth-frontend` screens must follow the contract.
