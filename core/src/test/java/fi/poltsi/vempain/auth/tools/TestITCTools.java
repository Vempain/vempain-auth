package fi.poltsi.vempain.auth.tools;

import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UserRepository;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static fi.poltsi.vempain.auth.tools.TestUserAccountTools.encryptPassword;
import static fi.poltsi.vempain.auth.tools.TestUserAccountTools.randomPassword;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@Getter
@AllArgsConstructor
@Component
public class TestITCTools {
	private final AclService     aclService;
	private final AclRepository  aclRepository;
	private final UserService    userService;
	private final UnitService    unitService;
	private final UserRepository userRepository;
	private final EntityManager  entityManager;

	@Transactional
	public Long generateAcl(Long userId, Long unitId, boolean read, boolean modify, boolean create, boolean delete) {
		long aclId = aclService.getNextAclId();
		return generateAclWithId(aclId, userId, unitId, read, modify, create, delete);
	}

	@Transactional
	public Long generateAclWithId(Long aclId, Long userId, Long unitId, boolean read, boolean modify, boolean create, boolean delete) {
		Acl acl = Acl.builder()
					 .aclId(aclId)
					 .createPrivilege(create)
					 .deletePrivilege(delete)
					 .modifyPrivilege(modify)
					 .readPrivilege(read)
					 .unitId(unitId)
					 .userId(userId)
					 .build();
		try {
			var newAcl = aclService.save(acl);
			aclId = newAcl.getAclId();
			log.info("Generated new ACL ID: {}", aclId);
		} catch (VempainAclException e) {
			log.error("Failed to create Acl");
			fail("Unable to create Acl");
		}

		return aclId;
	}

	@Transactional
	public List<Long> generateAcls(long counter) {
		var aclIdList = new ArrayList<Long>();

		for (long i = 0; i < counter; i++) {
			var userId  = generateUser();
			var nextAcl = aclService.getNextAclId();
			aclIdList.add(nextAcl);
			log.info("Creating acl with aclId: {}", nextAcl);
			generateAclWithId(nextAcl, userId, null, true, true, true, true);
			var unitId = generateUnit();
			generateAclWithId(nextAcl, null, unitId, true, true, true, true);
		}

		var acls = aclService.findAll();
		assertFalse(acls.isEmpty());
		return aclIdList;
	}

	/// //////////////// Acls end
	@Transactional
	public Long generateUser() {
		var password = randomPassword(16);
		return generateUser(password);
	}

	@Transactional
	public Long generateUser(String password) {
		// Note that here the user ID used to generate the ACL refers to the admin
		var aclId = generateAcl(ADMIN_ID, null, true, true, true, true);
		var user = UserAccount.builder()
							  .aclId(aclId)
							  .birthday(Instant.now()
											   .minus(20 * 365, ChronoUnit.DAYS))
							  .created(Instant.now()
											  .minus(1, ChronoUnit.HOURS))
							  .creator(ADMIN_ID)
							  .description("ITC generated user " + password)
							  .email("first." + password + "@test.tld")
							  .locked(false)
							  .loginName(password)
							  .modified(Instant.now())
							  .modifier(ADMIN_ID)
							  .name("Firstname " + password)
							  .nick(password)
							  .password(encryptPassword(password))
							  .pob("1111")
							  .privacyType(PrivacyType.PRIVATE)
							  .isPublic(false)
							  .street("")
							  .units(null)
							  .status(AccountStatus.ACTIVE)
							  .build();

		var newUser = userService.save(user);
		entityManager.flush();

		// Once we have generated a user, we can generate the ACL for the object
		var userAclID = generateAcl(newUser.getId(), null, true, true, true, true);
		// Then update the aclId on the user
		newUser.setAclId(userAclID);
		// We can also update the creator and modifier to be correct
		newUser.setCreator(newUser.getId());
		newUser.setModifier(newUser.getId());
		var newestUser = userService.save(newUser);
		entityManager.flush();
		// We also need to create a unit for the user
		var unitAclID = generateAcl(newestUser.getId(), null, true, true, true, true);
		log.info("New unit ACL ID for the user: {}", unitAclID);
		var unit = Unit.builder()
					   .description("ITC generated unit for user " + password)
					   .name("Unit " + password)
					   .aclId(unitAclID)
					   .locked(false)
					   .created(Instant.now()
									   .minus(1, ChronoUnit.HOURS))
					   .creator(newUser.getId())
					   .build();

		unitService.save(unit);

		entityManager.flush();
		return newUser.getId();
	}

	@Transactional
	public List<Long> generateUsers(Long count) {
		var idList = new ArrayList<Long>();

		for (long counter = 0L; counter < count; counter++) {
			idList.add(generateUser());
		}

		return idList;
	}

	@Transactional
	public Long generateUnit() {
		var randomString = TestUserAccountTools.randomLongString();
		var userId       = generateUser();
		// Once we have generated an user, we can generate the ACL for the object
		var aclId = generateAcl(userId, null, true, true, true, true);

		var unit = Unit.builder()
					   .aclId(aclId)
					   .description("ITC generated unit")
					   .name("Test unit " + randomString)
					   .locked(false)
					   .created(Instant.now())
					   .creator(userId)
					   .modified(null)
					   .modifier(null)
					   .build();
		var newUnit = unitService.save(unit);
		return newUnit.getId();
	}
	/////////////////// Unit end
}
