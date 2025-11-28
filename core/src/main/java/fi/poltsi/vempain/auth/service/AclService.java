package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class AclService {
	private final AclRepository         aclRepository;
	private final UserAccountRepository userAccountRepository;
	private final UnitRepository        unitRepository;
	private final AclIdGenerator aclIdGenerator;

	public List<Acl> findAll() {
        return aclRepository.findAll();
    }

    public List<Acl> findAclByAclId(long aclId) {
        return aclRepository.getAclByAclId(aclId);
    }

    public long getNextAclId() {
		// Use DB-backed hi/lo generator to avoid concurrency issues when available
		if (this.aclIdGenerator != null) {
			try {
				long next = aclIdGenerator.nextAclId();
				log.info("Next available ACL ID (via generator): {}", next);
				return next;
			} catch (Exception e) {
				log.warn("AclIdGenerator failed, falling back to repository-based allocator", e);
			}
		}

		// Fallback for unit tests or if generator is not available: repository-based next available id
		long fallback = aclRepository.getNextAvailableAclId();
		log.info("Next available ACL ID (via repository fallback): {}", fallback);
		return fallback;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteByAclId(long aclId) throws VempainEntityNotFoundException {
        Iterable<Acl> acls = findAclByAclId(aclId);

        if (acls == null || StreamSupport.stream(acls.spliterator(), false).findAny().isEmpty()) {
            log.error("No Acls found with id: {} to be deleted", aclId);
            throw new VempainEntityNotFoundException("ACL not found for deletion", "acl");
        }

        log.info("Deleting all acl with acl ID: {}", aclId);
        aclRepository.deleteAclsByAclId(aclId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Acl save(Acl acl) throws VempainAclException {
        verifyAcl(acl.getAclId(), acl.getUserId(), acl.getUnitId(), acl.isCreatePrivilege(), acl.isReadPrivilege(), acl.isModifyPrivilege(), acl.isDeletePrivilege());
        log.info("Saving ACL: {}", acl);

        return aclRepository.save(acl);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(Acl acl) throws VempainAclException {
        verifyAcl(acl.getAclId(), acl.getUserId(), acl.getUnitId(), acl.isCreatePrivilege(), acl.isReadPrivilege(), acl.isModifyPrivilege(), acl.isDeletePrivilege());

        if (acl.getId() == null) {
            log.error("Updating an ACL with no permission ID fails: {}", acl);
                throw new VempainAclException("Trying to update ACL with no permission ID");
        }

        aclRepository.update(acl.getId(), acl.getUserId(), acl.getUnitId(), acl.isReadPrivilege(), acl.isModifyPrivilege(), acl.isCreatePrivilege(), acl.isDeletePrivilege());
    }

	// TODO This does not currently work, we should also pass the old ACL ID for the object so that we can delete the old ACLs in case where all the requested
	// ACLs are new
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateFromRequestList(List<AclRequest> requests) throws VempainAclException {
        if (requests == null || requests.isEmpty()) {
            log.error("Request list did not contain any ACL, this would create an entity which nobody could modify: {}", requests);
            throw new VempainAclException("No ACL entries in request");
        }

        long aclId = requests.getFirst().getAclId();

		// If the first entry has an aclId of 0L, then they all are new and we need to fetch a new ACL ID, see the TODO above
		if (aclId < 1) {
			aclId = aclRepository.getNextAvailableAclId();
		}

		for (AclRequest request : requests) {
			// If this is a new ACL, we do not need to verify it, rather we set it
			if (request.getAclId() == 0L) {
				request.setAclId(aclId);
			} else if (request.getAclId() != aclId) {
				throw new VempainAclException("List of ACL request does not have all the same aclId");
			}
		}

		// We replace the old ACLs with the new ones
		try {
			deleteByAclId(aclId);
		} catch (VempainEntityNotFoundException e) {
			log.warn("When updating ACLs from request, previous ones were not found. This is not necessarily a bug as we also use this to" +
					 "create new ACLs");
		}

		for (AclRequest request : requests) {
			verifyAcl(request.getAclId(), request.getUser(), request.getUnit(), request.isCreatePrivilege(),
					  request.isReadPrivilege(), request.isModifyPrivilege(), request.isDeletePrivilege());

            var acl = Acl.builder()
                         .aclId(request.getAclId())
                         .userId(request.getUser())
                         .unitId(request.getUnit())
                         .readPrivilege(request.isReadPrivilege())
                         .modifyPrivilege(request.isModifyPrivilege())
                         .createPrivilege(request.isCreatePrivilege())
                         .deletePrivilege(request.isDeletePrivilege())
                         .build();
            save(acl);
        }
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

        List<Acl> oldAcls = findAclByAclId(aclId);

        if (!oldAcls.isEmpty()) {
			log.info("ACL ID {} already exists, deleting old ACLs", aclId);
			try {
				deleteByAclId(aclId);
			} catch (VempainEntityNotFoundException e) {
				log.warn("When saving ACLs from request, previous ones were not found. This is not necessarily a bug as we also use this to" +
						 "create new ACLs");
			}
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
            save(acl);
        }
    }

    public List<AclResponse> getAclResponses(Long aclId) {
        Iterable<Acl>          acls         = findAclByAclId(aclId);
        ArrayList<AclResponse> aclResponses = new ArrayList<>();

        for (Acl acl : acls) {
            aclResponses.add(acl.toResponse());
        }

        return aclResponses;
    }

	public void validateAbstractData(AbstractVempainEntity entity) throws VempainAbstractException {
		if (entity.getAclId() <= 0L) {
			log.error("Invalid ACL ID: {}", entity.getAclId());
			throw new VempainAbstractException("ACL ID is invalid");
		}

		if (entity.getCreator() == null ||
			entity.getCreator() < 1L) {
			log.error("Invalid creator ID: {}", entity.getCreator());
			throw new VempainAbstractException("Creator is missing or invalid");
		}

		if (entity.getCreated() == null) {
			log.error("Missing creation date");
			throw new VempainAbstractException("Created datetime is missing");
		}

		if (entity.getModifier() == null &&
			entity.getModified() != null) {
			log.error("Entity has modified date set, but not modifier");
			throw new VempainAbstractException("Modifier is missing while modified is set");
		}

		if (entity.getModifier() != null &&
			entity.getModified() == null) {
			log.error("Entity has modifier set, but not modified date");
			throw new VempainAbstractException("Modified datetime is missing while modifier is set");
		}

		if (entity.getModifier() != null &&
			entity.getModifier() < 1L) {
			log.error("Entity modifier is set but invalid: {}", entity.getModifier());
			throw new VempainAbstractException("Entity modifier is invalid");
		}

		if (entity.getModified() != null &&
			entity.getModified().isBefore(entity.getCreated())) {
			log.error("Entity modified date {} is before created date {}", entity.getModified(), entity.getCreated());
			throw new VempainAbstractException("Created datetime is more recent than modified");
		}
	}

	private void verifyAcl(Long aclId, Long userId, Long unitId, boolean createPrivilege, boolean readPrivilege, boolean modifyPrivilege,
						   boolean deletePrivilege) throws VempainAclException {
        if (aclId < 1) {
            log.error("Incorrect aclId value: {}", aclId);
            throw new VempainAclException("Incorrect aclId value");
        }

        if ((unitId == null) &&
            (userId == null)) {
            log.error("Invalid ACL with both user and unit null: {}", aclId);
            throw new VempainAclException("Both user and unit is null");
        }

        if ((unitId != null) &&
            (userId != null)) {
            log.error("Invalid ACL with both user and unit set: {}", aclId);
            throw new VempainAclException("Both user and unit are set");
        }

		if (userId != null) {
			log.info("User ID needs to be checked: {}", userId);
			var optionalUser = userAccountRepository.findById(userId);

			if (optionalUser.isEmpty()) {
				log.error("User does not exist: {}", userId);
				throw new VempainAclException("Invalid user ID given in the ACL");
			}
		}

		if (unitId != null) {
			var optionalUnit = unitRepository.findById(unitId);

			if (optionalUnit.isEmpty()) {
				log.error("Unit does not exist: {}", unitId);
				throw new VempainAclException("Invalid unit ID given in the ACL");
			}
		}

		if (!createPrivilege &&
            !modifyPrivilege &&
            !readPrivilege &&
            !deletePrivilege) {
            log.error("Invalid ACL with all privilege set to false: {}", aclId);
            throw new VempainAclException("All permissions set to false, this acl does not make any sense");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
	public long saveNewAclForObject(List<AclRequest> aclRequests) {
        long aclId = getNextAclId();

        try {
            saveAclRequests(aclId, aclRequests);
        } catch (Exception e) {
            switch (e.getMessage()) {
                case "New ACL ID is invalid" -> {
					log.error("Generated ACL ID is invalid: {}", aclId);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid ACL ID");
                }
                case "No ACL to save" -> {
					log.error("Object request did not contain a valid ACL request list: {}", aclRequests);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request contains no ACL list");
                }
                case "Trying to save an ACL ID that already exists" -> {
					log.error("Object request contained an already existing ACL ID: {}", aclRequests);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request contains already existing ACL ID");
                }
                default -> {
					log.error("Storing ACL list failed for unknown reason: {}", e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error");
                }
            }
        }

        return aclId;
	}

	public Iterable<Acl> findAllUserUnitAcls() {
        return aclRepository.findAllWithUserUnit();
	}

	public List<Long> findDuplicateAcls() {
        return aclRepository.findAclIdWithDuplicates();
	}

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteById(Long id) {
        aclRepository.deleteById(id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long createNewAcl(Long userId, Long unitId, boolean readPrivilege, boolean createPrivilege, boolean modifyPrivilege,
							 boolean deletePrivilege) throws VempainAclException {
        var aclId = aclRepository.getNextAvailableAclId();
        var acl = Acl.builder()
                     .aclId(aclId)
                     .userId(userId)
                     .unitId(unitId)
                     .readPrivilege(readPrivilege)
                     .createPrivilege(createPrivilege)
                     .modifyPrivilege(modifyPrivilege)
                     .deletePrivilege(deletePrivilege)
                     .build();
        save(acl);
        return acl.getAclId();
    }

	/**
	 * Atomically allocate a new acl_id and persist the Acl row in a single DB statement using the repository custom insert
	 * that leverages the database sequence. This avoids race conditions in concurrent environments.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Acl createUniqueAcl(Long userId, Long unitId, boolean readPrivilege, boolean createPrivilege, boolean modifyPrivilege,
							   boolean deletePrivilege) throws VempainAclException {
		// Validate minimal fields via verifyAcl by providing a temporary aclId (1) — validation requires aclId > 0 so skip verifyAcl here
		// Instead perform only necessary checks for user/unit existence
		if (userId == null && unitId == null) {
			throw new VempainAclException("Both user and unit are null");
		}

		if (userId != null) {
			if (userAccountRepository.findById(userId)
									 .isEmpty()) {
				throw new VempainAclException("Invalid user ID given in the ACL");
			}
		}
		if (unitId != null) {
			if (unitRepository.findById(unitId)
							  .isEmpty()) {
				throw new VempainAclException("Invalid unit ID given in the ACL");
			}
		}

		// Build the Acl entity to be inserted; aclId will be assigned by the DB sequence in the custom repository method
		Acl toInsert = Acl.builder()
						  .userId(userId)
						  .unitId(unitId)
						  .readPrivilege(readPrivilege)
						  .createPrivilege(createPrivilege)
						  .modifyPrivilege(modifyPrivilege)
						  .deletePrivilege(deletePrivilege)
						  .build();

		// Delegate to custom repository method which will use nextval(...) and RETURNING to atomically insert and return the Acl
		Acl saved = aclRepository.insertWithNextAclId(toInsert);
		return saved;
	}
}
