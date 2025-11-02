package fi.poltsi.vempain.auth.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_unit")
public class UserUnit implements Serializable {
	@EmbeddedId
	private UserUnitId  id;
	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("userId")
	private UserAccount user;
	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("unitId")
	private Unit        unit;
}
