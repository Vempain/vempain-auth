package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserAccountRepository userAccountRepository;
	private final UnitRepository        unitRepository;
	private final UnitMembershipService unitMembershipService;

	/**
	 * Loads the user with its effective units: the units it is a direct member of plus every unit containing those, at any depth,
	 * because unit membership is transitive through nested units. ACL rows granted to any of them apply to the user.
	 */
	@Override
	@Transactional
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = userAccountRepository.findByLoginName(username)
										.orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

		return UserDetailsImpl.build(user, effectiveUnits(user.getUnits()));
	}

	/**
	 * The direct units and every unit that contains them.
	 */
	public Set<Unit> effectiveUnits(Set<Unit> directUnits) {
		var units = new HashSet<Unit>(directUnits == null ? Set.of() : directUnits);
		var directIds = units.stream()
							 .map(Unit::getId)
							 .toList();
		var ancestorIds = new HashSet<>(unitMembershipService.findAncestorUnitIds(directIds));
		ancestorIds.removeAll(directIds);
		if (!ancestorIds.isEmpty()) {
			unitRepository.findAllById(ancestorIds)
						  .forEach(units::add);
		}
		return units;
	}
}
