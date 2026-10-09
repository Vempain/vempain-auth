package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.UnitUnit;
import fi.poltsi.vempain.auth.repository.UnitUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The nesting of units. A unit may contain other units; membership is transitive, so a user in a sub-unit is a member of every unit
 * above it. The structure must stay acyclic: {@link #wouldCreateCycle} is the backstop that rejects any set of sub-units that would make
 * a unit contain itself through a chain of units (A contains B contains C contains A).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnitMembershipService {
	private static final int          HIERARCHY_LOCK_CLASS_ID  = 0x56454D50;
	private static final int          HIERARCHY_LOCK_OBJECT_ID = 0x41494E54;
	private final UnitUnitRepository unitUnitRepository;
	private final JdbcTemplate        jdbcTemplate;

	/**
	 * Serializes hierarchy changes until the surrounding transaction commits, so cycle checks see prior committed changes.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockHierarchyMutations() {
		jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
			try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)")) {
				statement.setInt(1, HIERARCHY_LOCK_CLASS_ID);
				statement.setInt(2, HIERARCHY_LOCK_OBJECT_ID);
				statement.execute();
			}
			return null;
		});
	}

	/**
	 * IDs of the units that are direct sub-units of the given unit.
	 */
	public List<Long> findChildUnitIds(long unitId) {
		return unitUnitRepository.findByParentUnitId(unitId)
								 .stream()
								 .map(UnitUnit::getChildUnitId)
								 .toList();
	}

	/**
	 * IDs of every unit contained in the given units, directly or through other units (the given units themselves excluded unless
	 * reachable, which a consistent structure never allows).
	 */
	public Set<Long> findDescendantUnitIds(Collection<Long> unitIds) {
		return closure(unitIds, true);
	}

	/**
	 * IDs of every unit that contains one of the given units, directly or through other units. Together with the units themselves
	 * these are the units whose ACL rows apply to a member of the given units.
	 */
	public Set<Long> findAncestorUnitIds(Collection<Long> unitIds) {
		return closure(unitIds, false);
	}

	/**
	 * Whether making the given units the direct sub-units of {@code unitId} would let the unit contain itself: the unit is one of the
	 * sub-units, or the unit is contained (at any depth) in one of them.
	 */
	public boolean wouldCreateCycle(long unitId, Collection<Long> childUnitIds) {
		if (childUnitIds == null || childUnitIds.isEmpty()) {
			return false;
		}
		if (childUnitIds.contains(unitId)) {
			return true;
		}
		return findDescendantUnitIds(childUnitIds).contains(unitId);
	}

	/**
	 * Replaces the direct sub-units of a unit. The caller validates the units and the absence of cycles first.
	 */
	@Transactional
	public void replaceChildUnits(long unitId, Collection<Long> childUnitIds) {
		unitUnitRepository.deleteByParentUnitId(unitId);
		unitUnitRepository.flush();
		var unique = new LinkedHashSet<>(childUnitIds);
		for (var childId : unique) {
			unitUnitRepository.save(UnitUnit.builder()
											.parentUnitId(unitId)
											.childUnitId(childId)
											.build());
		}
		log.debug("Unit {} now has sub-units {}", unitId, unique);
	}

	private Set<Long> closure(Collection<Long> start, boolean downwards) {
		var visited = new HashSet<Long>();
		var queue   = new ArrayDeque<Long>(start == null ? List.of() : start);

		while (!queue.isEmpty()) {
			var batch = new HashSet<Long>();
			while (!queue.isEmpty()) {
				batch.add(queue.poll());
			}
			var edges = downwards ? unitUnitRepository.findByParentUnitIdIn(batch) : unitUnitRepository.findByChildUnitIdIn(batch);
			for (var edge : edges) {
				var next = downwards ? edge.getChildUnitId() : edge.getParentUnitId();
				if (visited.add(next)) {
					queue.add(next);
				}
			}
		}

		return visited;
	}
}
