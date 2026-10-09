package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.UnitUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UnitUnitRepository extends JpaRepository<UnitUnit, Long> {
	List<UnitUnit> findByParentUnitId(long parentUnitId);

	List<UnitUnit> findByParentUnitIdIn(Collection<Long> parentUnitIds);

	List<UnitUnit> findByChildUnitIdIn(Collection<Long> childUnitIds);

	void deleteByParentUnitId(long parentUnitId);
}
