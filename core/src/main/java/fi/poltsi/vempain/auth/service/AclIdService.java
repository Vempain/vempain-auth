package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.Acl;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AclIdService {
	private final EntityManager entityManager;

	public Acl generateNewAcl(Long userId, Long unitId, boolean read, boolean create, boolean modify, boolean delete) {
		var sqlString = """
				INSERT INTO acl (acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege) 
							VALUES ((SELECT nextval('acl_acl_id_seq')), :userId, :unitId, :readPriv, :createPriv, :modifyPriv, :deletePriv)
							RETURNING id, acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege
				""";

		var query = entityManager.createNativeQuery(sqlString, Acl.class);
		query.setParameter("userId", userId);
		query.setParameter("unitId", unitId);
		query.setParameter("readPriv", read);
		query.setParameter("createPriv", create);
		query.setParameter("modifyPriv", modify);
		query.setParameter("deletePriv", delete);
		var newAcl = (Acl) query.getSingleResult();
		log.debug("Generated new Acl: {}", newAcl);
		return newAcl;
	}
}
