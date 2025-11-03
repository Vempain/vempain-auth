package fi.poltsi.vempain.auth.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.entity.UserUnit;
import fi.poltsi.vempain.auth.entity.UserUnitId;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class TestUTCTools {

	// Acl
	public static Acl generateAcl(Long id, long aclId, Long userId, Long unitId) {
		return Acl.builder()
				  .id(id)
				  .aclId(aclId)
				  .userId(userId)
				  .unitId(unitId)
				  .readPrivilege(true)
				  .createPrivilege(true)
				  .modifyPrivilege(true)
				  .deletePrivilege(true)
				  .build();
	}

	public static List<Acl> generateAclList(long aclId, Long count, boolean units) {
		ArrayList<Acl> acls = new ArrayList<>();

		long id = 1L;

		for (long i = 1L; i <= count; i++) {
			Acl aclUser = generateAcl(id, aclId, i, null);
			acls.add(aclUser);
			id++;

			if (units) {
				Acl aclUnit = generateAcl(id, aclId, null, i);
				acls.add(aclUnit);
				id++;
			}
		}

		return acls;
	}

	public static List<Acl> generateAclList(long aclId, Long count) {
		return generateAclList(aclId, count, true);
	}

	public static List<AclRequest> generateAclRequestList(long aclId, Long count) {
		List<Acl> acls = generateAclList(aclId, count);
		return generateAclRequestListFromAcl(acls);
	}

	public static List<AclRequest> generateAclRequestListFromAcl(List<Acl> acls) {
		ArrayList<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			requests.add(AclRequest.builder()
								   .id(acl.getId())
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
		}

		return requests;
	}

	public static List<AclResponse> generateAclResponses(long aclId, long count) {
		ArrayList<AclResponse> aclResponses = new ArrayList<>();
		List<Acl>              acls         = generateAclList(aclId, count);

		for (Acl acl : acls) {
			aclResponses.add(acl.toResponse());
		}

		return aclResponses;
	}

	// User
	public static UserAccount generateUser(long userId) {
		log.info("Creating user with ID: {}", userId);
		return UserAccount.builder()
						  .id(userId)
						  .build();
	}

	public static List<UserAccount> generateUserList(long count) {
		ArrayList<UserAccount> users = new ArrayList<>();
		for (long i = 1; i <= count; i++) {
			users.add(generateUser(i));
		}

		return users;
	}

	// UserUnitId
	public static UserUnitId generateUserUnitId(long userId, long unitId) {
		return UserUnitId.builder()
						 .userId(userId)
						 .unitId(unitId)
						 .build();
	}

	// UserUnit
	public static UserUnit generateUserUnit(long userId, long unitId) {
		return UserUnit.builder()
					   .id(generateUserUnitId(userId, unitId))
					   .user(generateUser(userId))
					   .unit(generateUnit(unitId))
					   .build();
	}

	// Unit
	public static Unit generateUnit(long unitId) {
		return Unit.builder()
				   .id(unitId)
				   .name("Test unit " + unitId)
				   .build();
	}

	public static List<Unit> generateUnitList(long count) {
		ArrayList<Unit> units = new ArrayList<>();
		for (long i = 1; i <= count; i++) {
			units.add(generateUnit(i));
		}

		return units;
	}

	// Generic
	public static <T> T deepCopy(T original, Class<T> classType) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		try {
			return objectMapper.readValue(objectMapper.writeValueAsString(original), classType);
		} catch (JsonProcessingException e) {
			fail("Failed to deep copy FormComponent: " + e.getMessage());
			return null;
		}
	}
}
