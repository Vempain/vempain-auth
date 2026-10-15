package fi.poltsi.vempain.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.auth.entity.UserAccount;

@Repository
public interface UserAccountRepository extends CrudRepository<UserAccount, Long> {

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

    /**
     * The direct user members of a unit.
     */
    List<UserAccount> findByUnitsId(long unitId);
}
