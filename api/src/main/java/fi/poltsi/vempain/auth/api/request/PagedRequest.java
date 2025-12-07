package fi.poltsi.vempain.auth.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paged result request")
public class PagedRequest {
	@Schema(description = "Page number (0-based)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 0)
	private long page;

	@Schema(description = "Size of the page", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 1)
	private long size;

	@Schema(description = "Sort by field", example = "created_at", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@Nullable
	private String sortBy;

	@Schema(description = "Sort direction", example = "DESC", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@Nullable
	private Sort.Direction direction;

	@Schema(description = "Search query", example = "holiday", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@Nullable
	private String search;

	@Schema(description = "Whether the search is case sensitive", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@Nullable
	private Boolean caseSensitive;
}
