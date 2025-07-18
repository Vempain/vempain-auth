package fi.poltsi.vempain.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "acl")
public class Acl implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "acl_id", nullable = false)
	private long aclId;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "unit_id")
	private Long unitId;

	@Column(name = "create_privilege", nullable = false)
	private boolean createPrivilege;

	@Column(name = "read_privilege", nullable = false)
	private boolean readPrivilege;

	@Column(name = "modify_privilege", nullable = false)
	private boolean modifyPrivilege;

	@Column(name = "delete_privilege", nullable = false)
	private boolean deletePrivilege;

	@JsonIgnore
	public AclResponse toResponse() {
		return AclResponse.builder()
						  .id(this.id)
						  .aclId(this.aclId)
						  .user(this.userId)
						  .unit(this.unitId)
						  .createPrivilege(this.createPrivilege)
						  .readPrivilege(this.readPrivilege)
						  .modifyPrivilege(this.modifyPrivilege)
						  .deletePrivilege(this.deletePrivilege)
						  .build();
	}
}
