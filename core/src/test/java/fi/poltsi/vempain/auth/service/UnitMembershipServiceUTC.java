package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.UnitUnit;
import fi.poltsi.vempain.auth.repository.UnitUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cycle detection and closure computation over an in-memory unit nesting: 1 contains 2, 2 contains 3, 2 contains 4, 5 is separate.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnitMembershipServiceUTC {
	@Mock
	private UnitUnitRepository    unitUnitRepository;
	@InjectMocks
	private UnitMembershipService service;

	private static final List<UnitUnit> EDGES = List.of(edge(1L, 2L), edge(2L, 3L), edge(2L, 4L));

	private static UnitUnit edge(long parent, long child) {
		return UnitUnit.builder()
		               .parentUnitId(parent)
		               .childUnitId(child)
		               .build();
	}

	private void stubEdges() {
		when(unitUnitRepository.findByParentUnitIdIn(anyCollection())).thenAnswer(invocation -> {
			Collection<Long> parents = invocation.getArgument(0);
			return EDGES.stream()
			            .filter(e -> parents.contains(e.getParentUnitId()))
			            .toList();
		});
		when(unitUnitRepository.findByChildUnitIdIn(anyCollection())).thenAnswer(invocation -> {
			Collection<Long> children = invocation.getArgument(0);
			return EDGES.stream()
			            .filter(e -> children.contains(e.getChildUnitId()))
			            .toList();
		});
	}

	@Test
	void descendantsAndAncestorsFollowTheWholeChain() {
		stubEdges();

		assertEquals(Set.of(2L, 3L, 4L), service.findDescendantUnitIds(List.of(1L)));
		assertEquals(Set.of(3L, 4L), service.findDescendantUnitIds(List.of(2L)));
		assertEquals(Set.of(), service.findDescendantUnitIds(List.of(5L)));
		assertEquals(Set.of(2L, 1L), service.findAncestorUnitIds(List.of(3L)));
		assertEquals(Set.of(), service.findAncestorUnitIds(List.of(1L)));
		assertEquals(Set.of(), service.findAncestorUnitIds(null));
	}

	@Test
	void cyclesAreDetectedAtAnyDepth() {
		stubEdges();

		// A unit can not contain itself
		assertTrue(service.wouldCreateCycle(1L, List.of(1L)));
		// 3 is inside 2 which is inside 1: making 1 a member of 3 closes the loop 1 -> 2 -> 3 -> 1
		assertTrue(service.wouldCreateCycle(3L, List.of(1L)));
		assertTrue(service.wouldCreateCycle(2L, List.of(5L, 1L)));
		// Siblings and unrelated units are fine
		assertFalse(service.wouldCreateCycle(3L, List.of(4L)));
		assertFalse(service.wouldCreateCycle(1L, List.of(5L)));
		assertFalse(service.wouldCreateCycle(5L, List.of(1L)));
		assertFalse(service.wouldCreateCycle(1L, List.of()));
		assertFalse(service.wouldCreateCycle(1L, null));
	}

	@Test
	void replaceChildUnitsRewritesTheEdgesWithoutDuplicates() {
		service.replaceChildUnits(7L, List.of(8L, 9L, 8L));

		verify(unitUnitRepository).deleteByParentUnitId(7L);
		verify(unitUnitRepository).save(edge(7L, 8L));
		verify(unitUnitRepository).save(edge(7L, 9L));
	}
}
