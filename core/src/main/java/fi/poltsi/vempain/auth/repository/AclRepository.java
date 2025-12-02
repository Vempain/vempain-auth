package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.Acl;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
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
	void update(@Param(value = "id") Long id, @Param(value = "userId") Long userId, @Param(value = "unitId") Long unitId,
				@Param(value = "readPriv") boolean readPriv,
				@Param(value = "modifyPriv") boolean modifyPriv,
				@Param(value = "createPriv") boolean createPriv,
				@Param(value = "deletePriv") boolean deletePriv);

	@Modifying
	void deleteAclsByAclId(long aclId);

	@Query("FROM Acl WHERE userId IS NOT NULL AND unitId IS NOT NULL")
	Iterable<Acl> findAllWithUserUnit();

	@Query(value = "SELECT acl_id FROM acl GROUP BY acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id" +
				   " HAVING COUNT(*) > 1", nativeQuery = true)
	List<Long> findAclIdWithDuplicates();

	void deleteById(long id);

	void deleteAllByUnitId(long unitId);

	@Query(value = "SELECT nextval('acl_acl_id_seq')", nativeQuery = true)
	Long getNextAclId();

	@Modifying
	@Query(nativeQuery = true, value =
			"""
					INSERT INTO acl (acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege) 
								VALUES ((SELECT nextval('acl_acl_id_seq')), :userId, :unitId, :readPriv, :createPriv, :modifyPriv, :deletePriv)
								RETURNING id
					""")
	Long createNewAclWithNewAclId(@Param(value = "userId") Long userId,
								  @Param(value = "unitId") Long unitId,
								  @Param(value = "readPriv") boolean readPriv,
								  @Param(value = "modifyPriv") boolean modifyPriv,
								  @Param(value = "createPriv") boolean createPriv,
								  @Param(value = "deletePriv") boolean deletePriv);
}
