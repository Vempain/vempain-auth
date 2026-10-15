package fi.poltsi.vempain.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.auth.entity.Unit;

@Repository
public interface UnitRepository extends CrudRepository<Unit, Long> {

    void deleteUnitById(long id);
}
