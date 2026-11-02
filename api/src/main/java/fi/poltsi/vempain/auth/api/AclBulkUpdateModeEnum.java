package fi.poltsi.vempain.auth.api;

/** How a bulk ACL update treats the rows the ACLs already have */
public enum AclBulkUpdateModeEnum {
    /** The entries are added; an entry for a user or unit that already has a row replaces that row's privileges, other rows stay */
    ADD,
    /** The entries become the whole ACL: every existing row is removed first */
    REPLACE
}
