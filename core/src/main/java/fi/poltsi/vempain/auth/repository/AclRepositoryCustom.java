package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.Acl;

public interface AclRepositoryCustom {
	/**
	 * Atomically inserts a new ACL row using the database sequence to generate a unique acl_id and
	 * returns the persisted Acl (including the assigned aclId and generated id).
	 */
	Acl insertWithNextAclId(Acl aclData);
}

