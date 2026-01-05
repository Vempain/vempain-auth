package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Generic paged response wrapper")
public class PagedResponse<T> {

	@Schema(description = "List of items on the current page")
	@NotNull
	private List<T> content;

	@Schema(description = "Current page index (0-based)", example = "0")
	@Min(0)
	private int page;

	@Schema(description = "Requested page size", example = "50")
	@Min(1)
	private int size;

	@Schema(description = "Total amount of items", example = "70000")
	@Min(0)
	private long totalElements;

	@Schema(description = "Total number of pages", example = "1400")
	@Min(0)
	private int totalPages;

	@Schema(description = "Is this the first page", example = "true")
	private boolean first;

	@Schema(description = "Is this the last page", example = "false")
	private boolean last;

	@Schema(description = "Is this the response empty", example = "false")
	private boolean empty;

	// Convenience factory
	public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
		var pr = new PagedResponse<T>();
		pr.setContent(content);
		pr.setPage(page);
		pr.setSize(size);
		pr.setTotalElements(totalElements);
		pr.setTotalPages(totalPages);
		pr.setFirst(first);
		pr.setLast(last);
		pr.setEmpty(content == null || content.isEmpty());
		return pr;
	}
}

