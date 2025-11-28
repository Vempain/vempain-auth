package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.entity.Acl;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class AclRepositoryImpl implements AclRepositoryCustom {
	private final NamedParameterJdbcTemplate jdbc;

	public AclRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	private static final RowMapper<Acl> ACL_ROW_MAPPER = new RowMapper<>() {
		@Override
		public Acl mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			var acl = new Acl();
			acl.setId(resultSet.getLong("id"));
			acl.setAclId(resultSet.getLong("acl_id"));
			var uid = resultSet.getLong("user_id");

			if (resultSet.wasNull()) {
				acl.setUserId(null);
			} else {
				acl.setUserId(uid);
			}

			var unitId = resultSet.getLong("unit_id");

			if (resultSet.wasNull()) {
				acl.setUnitId(null);
			} else {
				acl.setUnitId(unitId);
			}

			acl.setCreatePrivilege(resultSet.getBoolean("create_privilege"));
			acl.setReadPrivilege(resultSet.getBoolean("read_privilege"));
			acl.setModifyPrivilege(resultSet.getBoolean("modify_privilege"));
			acl.setDeletePrivilege(resultSet.getBoolean("delete_privilege"));

			return acl;
		}
	};

	@Override
	@Transactional
	public Acl insertWithNextAclId(Acl aclData) {
		String sql = "INSERT INTO acl (acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege) " +
					 "VALUES (nextval('acl_acl_id_seq'), :userId, :unitId, :read, :create, :modify, :delete) RETURNING id, acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege";

		var params = new MapSqlParameterSource();
		if (aclData.getUserId() != null) {
			params.addValue("userId", aclData.getUserId());
		} else {
			params.addValue("userId", null);
		}
		if (aclData.getUnitId() != null) {
			params.addValue("unitId", aclData.getUnitId());
		} else {
			params.addValue("unitId", null);
		}
		params.addValue("read", aclData.isReadPrivilege());
		params.addValue("create", aclData.isCreatePrivilege());
		params.addValue("modify", aclData.isModifyPrivilege());
		params.addValue("delete", aclData.isDeletePrivilege());

		return jdbc.queryForObject(sql, params, ACL_ROW_MAPPER);
	}
}
