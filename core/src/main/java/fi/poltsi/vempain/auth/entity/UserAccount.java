package fi.poltsi.vempain.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_account")
@ToString(callSuper = true)
public class UserAccount extends AbstractVempainEntity {
	@Column(name = "public_account", nullable = false)
	private boolean       isPublic;

	@Column(name = "name", nullable = false)
	private String        name;

	@Column(name = "nick", nullable = false, unique = true)
	private String        nick;

	@Column(name = "login_name", nullable = false, unique = true)
	private String        loginName;

	@Column(name = "password", nullable = false)
	private String        password;

	@Enumerated(EnumType.STRING)
	@Column(name = "priv_type", nullable = false)
	private PrivacyType privacyType;

	@Column(name = "email", nullable = false, unique = true)
	private String        email;

	@Column(name = "street")
	private String        street;

	@Column(name = "pob")
	private String        pob;

	@Column(name = "birthday", nullable = false)
	private Instant birthday;

	@Column(name = "description")
	private String        description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private AccountStatus status;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_unit", joinColumns = @JoinColumn(name = "user_id"),
			   inverseJoinColumns = @JoinColumn(name = "unit_id"))
	@Builder.Default
	private Set<Unit> units = new HashSet<>();

	@JsonIgnore
	public UserResponse getUserResponse() {
		return UserResponse.builder()
						   .id(this.id)
						   .name(this.name)
						   .nick(this.nick)
						   .loginName(this.loginName)
						   .description(this.description)
						   .birthday(this.birthday)
						   .street(this.street)
						   .pob(this.pob)
						   .email(this.email)
						   .privacyType(this.privacyType)
						   .privateUser(this.isPublic)
						   .status(this.status)
						   .acls(null)
						   .creator(this.creator)
						   .created(this.created)
						   .modifier(this.modifier)
						   .modified(this.modified)
						   .build();
	}
}
