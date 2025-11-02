package fi.poltsi.vempain.auth.it;

import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AclServiceIntegrationITC.TestApp.class)
class AclServiceIntegrationITC extends BasePostgresContainerSetup {

	@Autowired
	private AclService     aclService;
	@Autowired
	private AclRepository  aclRepository;
	@Autowired
	private UnitRepository unitRepository;

	@Test
	void create_find_delete_acl_flow() throws Exception {
		var unit = Unit.builder()
					   .name("ACL_UNIT")
					   // add required audit fields
					   .creator(1L)
					   .created(Instant.now())
					   // if Unit extends AbstractVempainEntity, aclId must be positive
					   .aclId(1L)
					   .build();
		unit = unitRepository.save(unit);

		Long aclId = aclService.createNewAcl(
				null,               // userId
				unit.getId(),       // unitId
				true,               // readPrivilege
				false,              // createPrivilege
				false,              // modifyPrivilege
				false               // deletePrivilege
		);

		var stored = aclService.findAclByAclId(aclId);
		assertThat(stored).isNotEmpty();
		assertThat(stored.getFirst()
						 .getUnitId()).isEqualTo(unit.getId());

		aclService.deleteByAclId(aclId);
		assertThat(aclRepository.getAclByAclId(aclId)).isEmpty();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
	@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
	@ComponentScan(basePackages = {
			"fi.poltsi.vempain.auth.service",
			"fi.poltsi.vempain.auth.repository"
	})
	static class TestApp {
	}
}
