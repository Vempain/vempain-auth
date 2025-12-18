package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitService {
	private final UnitRepository unitRepository;
	private final AclRepository  aclRepository;

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
		return unitResponse;
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

		var unitResponse = newUnit.getUnitResponse();
		populateWithAcl(aclId, unitResponse);
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

		var unitResponse = newUnit.getUnitResponse();
		populateWithAcl(aclId, unitResponse);
		return unitResponse;
	}
}
