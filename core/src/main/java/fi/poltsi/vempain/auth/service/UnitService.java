package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
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
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitService {
	private final UnitRepository        unitRepository;
	private final AclRepository         aclRepository;
	private final UserAccountRepository userAccountRepository;
	private final UnitMembershipService unitMembershipService;

	public Iterable<Unit> findAll() {
		return unitRepository.findAll();
	}

	public UnitResponse findById(Long unitId) throws VempainEntityNotFoundException {
		var optionalUnit = unitRepository.findById(unitId);

		if (optionalUnit.isEmpty()) {
			log.error("Tried to fetch a non-existing unit ID: {}", unitId);
			throw new VempainEntityNotFoundException("Unit not found for retrieval", "Unit");
		}

		var unitResponse = optionalUnit.get()
									   .getUnitResponse();
		populateWithAcl(optionalUnit.get()
									.getAclId(), unitResponse);
		populateWithMembers(unitResponse);
		return unitResponse;
	}

	/**
	 * Every unit with its direct member ids (no ACL rows).
	 */
	public List<UnitResponse> findAllResponses() {
		var responses = new ArrayList<UnitResponse>();
		unitRepository.findAll()
					  .forEach(unit -> {
						  var response = unit.getUnitResponse();
						  populateWithMembers(response);
						  responses.add(response);
					  });
		return responses;
	}

	/**
	 * A page of units sorted by name (or id with {@code sort_by=id}) and filtered by name through {@code search}.
	 */
	public PagedResponse<UnitResponse> findPaged(PagedRequest request) {
		return PagingTools.page(findAllResponses(), request, unit -> List.of(unit.getName()), UnitResponse::getName, UnitResponse::getId);
	}

	/**
	 * Fills the direct member user and unit ids of the unit.
	 */
	public void populateWithMembers(UnitResponse unitResponse) {
		unitResponse.setUserIds(userAccountRepository.findByUnits_Id(unitResponse.getId())
													 .stream()
													 .map(UserAccount::getId)
													 .sorted()
													 .toList());
		unitResponse.setUnitIds(unitMembershipService.findChildUnitIds(unitResponse.getId())
													 .stream()
													 .sorted()
													 .toList());
	}

	/**
	 * Replaces the direct members of a unit. Null lists leave that kind of membership untouched. Every member must exist, a unit can not
	 * be its own member and no chain of units may lead back to the unit (the backstop for the frontend's own check).
	 *
	 * @throws ResponseStatusException 400 for an unknown member or a circular membership
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateMembers(long unitId, List<Long> userIds, List<Long> childUnitIds) {
		if (childUnitIds != null) {
			var requested = new LinkedHashSet<>(childUnitIds);
			requested.remove(null);
			if (requested.contains(unitId)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unit can not be a member of itself");
			}
			for (var childId : requested) {
				if (unitRepository.findById(childId)
								  .isEmpty()) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown member unit " + childId);
				}
			}
			if (unitMembershipService.wouldCreateCycle(unitId, requested)) {
				log.warn("Rejected circular unit membership: unit {} would contain itself through {}", unitId, requested);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circular unit membership: the unit would contain itself");
			}
			unitMembershipService.replaceChildUnits(unitId, requested);
		}

		if (userIds != null) {
			var unit = unitRepository.findById(unitId)
									 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No unit was found with given ID"));
			var requested = new LinkedHashSet<>(userIds);
			requested.remove(null);
			var members = new ArrayList<UserAccount>();
			for (var userId : requested) {
				members.add(userAccountRepository.findById(userId)
												 .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown member user " + userId)));
			}

			// The user account owns the join table, so memberships are changed through the users
			for (var current : userAccountRepository.findByUnits_Id(unitId)) {
				if (!requested.contains(current.getId()) && current.getUnits() != null) {
					current.getUnits()
						   .removeIf(member -> member.getId()
													 .equals(unitId));
					userAccountRepository.save(current);
				}
			}
			for (var member : members) {
				if (member.getUnits() == null) {
					member.setUnits(new java.util.HashSet<>());
				}
				if (member.getUnits()
						  .stream()
						  .noneMatch(existing -> existing.getId()
														 .equals(unitId))) {
					member.getUnits()
						  .add(unit);
					userAccountRepository.save(member);
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteById(long unitId) {
		var optionalUnit = unitRepository.findById(unitId);

		if (optionalUnit.isEmpty()) {
			log.error("Could not find any unit by id {}", unitId);
			return;
		}

		var unit = optionalUnit.get();
		// Delete the unit ACL first
		aclRepository.deleteAclsByAclId(unit.getAclId());
		// We don't have a cascade delete for ACLs, so we need to delete all ACLs that refer to the unit manually
		aclRepository.deleteAllByUnitId(unitId);
		// Finally we can delete the unit itself
		unitRepository.delete(unit);
	}

	public Unit save(Unit unit) {
		return unitRepository.save(unit);
	}

	private void populateWithAcl(long aclId, UnitResponse unitResponse) {
		var acls         = aclRepository.getAclByAclId(aclId);
		var aclResponses = new ArrayList<AclResponse>();
		for (var acl : acls) {
			aclResponses.add(acl.toResponse());
		}

		unitResponse.setAcls(aclResponses);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UnitResponse createUnit(UnitRequest unitRequest) {
		var aclId = aclRepository.getNextAclId();

		try {
			saveAclRequests(aclId, unitRequest.getAcls());
		} catch (VempainAclException e) {
			log.warn("Failed to save ACLs for new unit: {}", unitRequest.getName());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACL request list is corrupted");
		}

		var unit = Unit.builder()
					   .name(unitRequest.getName())
					   .description(unitRequest.getDescription())
					   .aclId(aclId)
					   .creator(AuthTools.getCurrentUserId())
					   .created(Instant.now())
					   .build();

		var newUnit = unitRepository.save(unit);
		updateMembers(newUnit.getId(), unitRequest.getUserIds(), unitRequest.getUnitIds());

		var unitResponse = newUnit.getUnitResponse();
		populateWithAcl(aclId, unitResponse);
		populateWithMembers(unitResponse);
		return unitResponse;
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

	@Transactional
	public UnitResponse updateUnit(Long unitId, UnitRequest unitRequest) {
		var optionalUnit = unitRepository.findById(unitId);

		if (optionalUnit.isEmpty()) {
			log.error("Could not find any unit by id {}", unitId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No unit was found with given ID");
		}

		var unit  = optionalUnit.get();
		var aclId = unit.getAclId();

		try {
			saveAclRequests(aclId, unitRequest.getAcls());
		} catch (VempainAclException e) {
			log.warn("Failed to save ACLs for unit: {}", unitRequest.getName());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACL request list is corrupted");
		}

		unit.setName(unitRequest.getName());
		unit.setDescription(unitRequest.getDescription());
		unit.setModifier(AuthTools.getCurrentUserId());
		unit.setModified(Instant.now());

		var newUnit = unitRepository.save(unit);
		updateMembers(newUnit.getId(), unitRequest.getUserIds(), unitRequest.getUnitIds());

		var unitResponse = newUnit.getUnitResponse();
		populateWithAcl(aclId, unitResponse);
		populateWithMembers(unitResponse);
		return unitResponse;
	}
}
