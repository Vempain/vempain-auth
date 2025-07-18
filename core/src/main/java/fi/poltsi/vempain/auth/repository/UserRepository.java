package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserAccount, Long> {
	Optional<UserAccount> findByName(String name);

	Optional<UserAccount> findByLoginName(String loginName);

	Optional<UserAccount> findById(long id);

	@Modifying
	@Query(value = "UPDATE UserAccount SET locked = true WHERE id = :id")
	void lockByUserId(@Param("id") long id);

	@Modifying
	@Query(value = "UPDATE UserAccount SET locked = :locked WHERE id = :userId")
	void updateLockedByUserId(@Param("userId") Long userId, @Param("locked") boolean locked);

	Optional<UserAccount> findByAclId(long aclId);
}
