package fi.poltsi.vempain.auth.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractVempainEntity implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	protected Long    id;
	@Basic
	@Column(name = "acl_id", unique = true, nullable = false)
	protected long    aclId;
	@Basic
	@Column(name = "locked", nullable = false)
	protected boolean locked;
	@Column(name = "creator", nullable = false)
	protected Long    creator;
	@Column(name = "created", nullable = false)
	protected Instant created;
	@Column(name = "modifier")
	protected Long    modifier;
	@Column(name = "modified")
	protected Instant modified;
}
