package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.Acl;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public interface AclRepository extends ListPagingAndSortingRepository<Acl, Long>, CrudRepository<Acl, Long> {
	@Query("FROM Acl ORDER BY aclId ASC")
	List<Acl> findAll();

	List<Acl> getAclByAclId(long id);

	@Modifying
	@Query(value = "UPDATE Acl SET userId = :userId, unitId = :unitId, readPrivilege = :readPriv, modifyPrivilege = :modifyPriv, createPrivilege = :createPriv, deletePrivilege = :deletePriv " +
				   "WHERE id = :id")
	void update(Long id, Long userId, Long unitId, boolean readPriv, boolean modifyPriv, boolean createPriv, boolean deletePriv);

	@Modifying
	void deleteAclsByAclId(long aclId);

	@Query("SELECT CASE WHEN MAX(aclId) IS NULL THEN 1 ELSE (MAX(aclId) + 1) END AS next FROM Acl")
	long getNextAvailableAclId();

	@Query("FROM Acl WHERE userId IS NOT NULL AND unitId IS NOT NULL")
	Iterable<Acl> findAllWithUserUnit();

	@Query(value = "SELECT acl_id FROM acl GROUP BY acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id" +
				   " HAVING COUNT(*) > 1", nativeQuery = true)
	List<Long> findAclIdWithDuplicates();

	void deleteById(Long id);

	void deleteAllByUnitId(long unitId);
}
