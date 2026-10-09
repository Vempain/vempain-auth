package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.AuthTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
	private final UserAccountRepository userAccountRepository;
	private final AclRepository  aclRepository;
	private final UnitRepository unitRepository;

	public Iterable<UserAccount> findAll() {
		return userAccountRepository.findAll();
	}

	public UserResponse findUserResponseById(Long userId) {
		var optionalUser = findById(userId);
		var userResponse = optionalUser.map(UserAccount::getUserResponse).orElse(null);

		if (userResponse != null) {
			populateWithAcl(optionalUser.get().getAclId(), userResponse);
			populateWithUnits(optionalUser.get(), userResponse);
		}

		return userResponse;
	}

	/**
	 * Every user with its direct unit memberships (no ACL rows).
	 */
	public List<UserResponse> findAllResponses() {
		var responses = new ArrayList<UserResponse>();
		userAccountRepository.findAll()
							 .forEach(user -> {
								 var response = user.getUserResponse();
								 populateWithUnits(user, response);
								 responses.add(response);
							 });
		return responses;
	}

	/**
	 * A page of users sorted by name (or id with {@code sort_by=id}) and filtered by name or login name through {@code search}.
	 */
	public PagedResponse<UserResponse> findPaged(PagedRequest request) {
		return PagingTools.page(findAllResponses(), request, user -> java.util.Arrays.asList(user.getName(), user.getLoginName()),
								UserResponse::getName, UserResponse::getId);
	}

	private void populateWithUnits(UserAccount user, UserResponse userResponse) {
		userResponse.setUnitIds(user.getUnits() == null ? List.of() : user.getUnits()
																		  .stream()
																		  .map(Unit::getId)
																		  .sorted()
																		  .toList());
	}

	/**
	 * Replaces the direct unit memberships of a user; a null list leaves them untouched.
	 *
	 * @throws ResponseStatusException 400 for an unknown unit
	 */
	private void applyUnits(UserAccount user, List<Long> unitIds) {
		if (unitIds == null) {
			return;
		}
		var units = new HashSet<Unit>();
		for (var unitId : new LinkedHashSet<>(unitIds)) {
			if (unitId == null) {
				continue;
			}
			units.add(unitRepository.findById(unitId)
									.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown unit " + unitId)));
		}
		user.setUnits(units);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserResponse createUser(UserRequest userRequest) {
		// We need to manually handle the storing of the ACL since it is not a part of the user entity
		var aclId = aclRepository.getNextAclId();
		try {
			saveAclRequests(aclId, userRequest.getAcls());
		} catch (VempainAclException e) {
			log.warn("Failed to save ACLs for new user: {}", userRequest.getLoginName());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACL request list is corrupted");
		}

		if (!AuthTools.passwordCheck(userRequest.getPassword())) {
			log.error("Given password '{}' is not valid as it does not fulfill all the requirements for complexity", userRequest.getPassword());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is not valid");
		}

		var passwordHash = AuthTools.passwordHash(userRequest.getPassword());

		var user = UserAccount.builder()
							  .privacyType(userRequest.getPrivacyType())
							  .isPublic(userRequest.isPrivateUser())
							  .name(userRequest.getName())
							  .nick(userRequest.getNick())
							  .loginName(userRequest.getLoginName())
							  .email(userRequest.getEmail())
							  .street(userRequest.getStreet())
							  .pob(userRequest.getPob())
							  .birthday(userRequest.getBirthday())
							  .description(userRequest.getDescription())
							  .password(passwordHash)
							  .status(fi.poltsi.vempain.auth.api.AccountStatus.REGISTERED)
							  .aclId(aclId)
							  .creator(AuthTools.getCurrentUserId())
							  .created(Instant.now())
							  .build();
		applyUnits(user, userRequest.getUnitIds());

		var newUser = userAccountRepository.save(user);
		var userResponse = newUser.getUserResponse();
		populateWithAcl(aclId, userResponse);
		populateWithUnits(newUser, userResponse);
		return userResponse;
	}

	@Transactional
	public UserResponse updateUser(Long userId, UserRequest userRequest) {
		var optionalUser = findById(userId);

		if (optionalUser.isEmpty()) {
			log.error("Could not find any user by id {}", userId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user was found with given ID");
		}

		var user  = optionalUser.get();
		var aclId = user.getAclId();

		try {
			saveAclRequests(aclId, userRequest.getAcls());
		} catch (VempainAclException e) {
			log.warn("Failed to save ACLs for new user: {}", userRequest.getLoginName());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACL request list is corrupted");
		}

		user.setBirthday(userRequest.getBirthday());
		user.setDescription(userRequest.getDescription());
		user.setEmail(userRequest.getEmail());
		user.setLocked(userRequest.isLocked());
		user.setLoginName(userRequest.getLoginName());
		user.setName(userRequest.getName());
		user.setNick(userRequest.getNick());
		user.setPob(userRequest.getPob());
		user.setPrivacyType(userRequest.getPrivacyType());
		user.setPublic(userRequest.isPrivateUser());
		user.setStreet(userRequest.getStreet());

		if (userRequest.getPassword() != null && !userRequest.getPassword()
		                                                     .isBlank()) {
			if (!AuthTools.passwordCheck(userRequest.getPassword())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is not valid");
			}
			user.setPassword(AuthTools.passwordHash(userRequest.getPassword()));
		}

		applyUnits(user, userRequest.getUnitIds());
		user.setModifier(AuthTools.getCurrentUserId());
		user.setModified(Instant.now());

		var newUser = userAccountRepository.save(user);
		var userResponse = newUser.getUserResponse();
		populateWithAcl(aclId, userResponse);
		populateWithUnits(newUser, userResponse);
		return userResponse;
	}

	public Optional<UserAccount> findById(Long userId) {
		return userAccountRepository.findById(userId);
	}

	public Optional<UserAccount> findByLogin(String login) {
		return userAccountRepository.findByLoginName(login);
	}

	// TODO We should instead use a status field and mark the user as closed instead of deleting it since the user ID is located in many places
	@Transactional(propagation = Propagation.REQUIRED)
	public void lockUser(long userId) {
		userAccountRepository.lockByUserId(userId);
	}

	public UserAccount save(UserAccount userAccount) {
		return userAccountRepository.save(userAccount);
	}

	private void populateWithAcl(long aclId, UserResponse userResponse) {
		var acls         = aclRepository.getAclByAclId(aclId);
		var aclResponses = new ArrayList<AclResponse>();
		for (var acl : acls) {
			aclResponses.add(acl.toResponse());
		}

		userResponse.setAcls(aclResponses);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveAclRequests(Long aclId, List<AclRequest> acls) throws VempainAclException {
		if (aclId == null || aclId < 1) {
			log.error("ACL ID is invalid: {}", aclId);
			throw new VempainAclException("New ACL ID is invalid");
		}

		if (acls == null || acls.isEmpty()) {
			log.error("ACL array is empty");
			throw new VempainAclException("No ACL to save");
		}

		List<Acl> oldAcls = aclRepository.getAclByAclId(aclId);

		if (!oldAcls.isEmpty()) {
			log.debug("ACL ID {} already exists, deleting old ACLs", aclId);
			aclRepository.deleteAclsByAclId(aclId);
		}

		for (AclRequest aclRequest : acls) {

			var acl = Acl.builder()
						 .aclId(aclId)
						 .userId(aclRequest.getUser())
						 .unitId(aclRequest.getUnit())
						 .readPrivilege(aclRequest.isReadPrivilege())
						 .createPrivilege(aclRequest.isCreatePrivilege())
						 .modifyPrivilege(aclRequest.isModifyPrivilege())
						 .deletePrivilege(aclRequest.isDeletePrivilege())
						 .build();
			aclRepository.save(acl);
		}
	}
}
