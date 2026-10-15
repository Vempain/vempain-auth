package fi.poltsi.vempain.auth.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.auth.entity.UnitUnit;

@Repository
public interface UnitUnitRepository extends JpaRepository<UnitUnit, Long> {

    List<UnitUnit> findByParentUnitId(long parentUnitId);

    List<UnitUnit> findByParentUnitIdIn(Collection<Long> parentUnitIds);

    List<UnitUnit> findByChildUnitIdIn(Collection<Long> childUnitIds);

    void deleteByParentUnitId(long parentUnitId);
}
