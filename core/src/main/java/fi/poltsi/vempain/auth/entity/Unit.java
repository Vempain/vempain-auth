package fi.poltsi.vempain.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "unit")
public class Unit extends AbstractVempainEntity {
	@Column(name = "name", nullable = false)
	private String name;
	@Column(name = "description")
	private String description;

	@JsonIgnore
	public UnitResponse getUnitResponse() {
		return UnitResponse.builder()
						   .id(this.id)
						   .name(this.name)
						   .description(this.description)
						   .acls(null)
						   .creator(this.creator)
						   .created(this.created)
						   .modifier(this.modifier)
						   .modified(this.modified)
						   .build();
	}
}
