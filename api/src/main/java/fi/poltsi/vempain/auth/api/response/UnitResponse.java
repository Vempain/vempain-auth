package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting a unit item")
public class UnitResponse extends AbstractResponse {
	@Schema(description = "Unit name", example = "users")
	private String name;
	@Schema(description = "Unit description", example = "Normal users")
	private String description;
	@Schema(description = "IDs of the users that are direct members of the unit", example = "[3, 7]")
	private List<Long> userIds;
	@Schema(description = "IDs of the units that are direct members (sub-units) of the unit", example = "[12]")
	private List<Long> unitIds;
}
