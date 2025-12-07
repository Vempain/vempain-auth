package fi.poltsi.vempain.auth.api.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.data.domain.Sort;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@JsonTest
class PagedRequestJTC {
	@Autowired
	private JacksonTester<PagedRequest> jacksonTester;

	@Test
	void pagedRequestWithAllFieldsTest() throws IOException {
		PagedRequest pagedRequest = PagedRequest.builder()
												.page(0)
												.size(25)
												.sortBy("created_at")
												.direction(Sort.Direction.DESC)
												.search("holiday")
												.caseSensitive(false)
												.build();
		JsonContent<PagedRequest> result = this.jacksonTester.write(pagedRequest);

		// Verify snake_case naming is used
		assertThat(result).hasJsonPathNumberValue("@.page")
						  .hasJsonPathNumberValue("@.size")
						  .hasJsonPathStringValue("@.sort_by")
						  .hasJsonPathStringValue("@.direction")
						  .hasJsonPathStringValue("@.search")
						  .hasJsonPathBooleanValue("@.case_sensitive");

		// Verify actual values
		assertThat(result).extractingJsonPathNumberValue("@.page")
						  .isEqualTo(0);
		assertThat(result).extractingJsonPathNumberValue("@.size")
						  .isEqualTo(25);
		assertThat(result).extractingJsonPathStringValue("@.sort_by")
						  .isEqualTo("created_at");
		assertThat(result).extractingJsonPathStringValue("@.direction")
						  .isEqualTo("DESC");
		assertThat(result).extractingJsonPathStringValue("@.search")
						  .isEqualTo("holiday");
		assertThat(result).extractingJsonPathBooleanValue("@.case_sensitive")
						  .isEqualTo(false);
	}

	@Test
	void pagedRequestWithMinimalFieldsTest() throws IOException {
		PagedRequest pagedRequest = PagedRequest.builder()
												.page(1)
												.size(10)
												.build();
		JsonContent<PagedRequest> result = this.jacksonTester.write(pagedRequest);

		// Verify required fields are present with snake_case naming
		assertThat(result).hasJsonPathNumberValue("@.page")
						  .hasJsonPathNumberValue("@.size");

		// Verify optional fields are null
		assertThat(result).hasEmptyJsonPathValue("@.sort_by")
						  .hasEmptyJsonPathValue("@.direction")
						  .hasEmptyJsonPathValue("@.search")
						  .hasEmptyJsonPathValue("@.case_sensitive");

		// Verify actual values
		assertThat(result).extractingJsonPathNumberValue("@.page")
						  .isEqualTo(1);
		assertThat(result).extractingJsonPathNumberValue("@.size")
						  .isEqualTo(10);
	}

	@Test
	void pagedRequestBuilderPatternTest() {
		// Test builder pattern functionality
		PagedRequest request = PagedRequest.builder()
										   .page(2)
										   .size(50)
										   .sortBy("name")
										   .direction(Sort.Direction.ASC)
										   .search("test")
										   .caseSensitive(true)
										   .build();

		assertThat(request.getPage()).isEqualTo(2);
		assertThat(request.getSize()).isEqualTo(50);
		assertThat(request.getSortBy()).isEqualTo("name");
		assertThat(request.getDirection()).isEqualTo(Sort.Direction.ASC);
		assertThat(request.getSearch()).isEqualTo("test");
		assertThat(request.getCaseSensitive()).isEqualTo(true);
	}

	@Test
	void pagedRequestWithBoundaryValuesTest() throws IOException {
		// Test minimum valid values for @Min constraints
		PagedRequest pagedRequest = PagedRequest.builder()
												.page(0)  // @Min(0)
												.size(1)  // @Min(1)
												.build();
		JsonContent<PagedRequest> result = this.jacksonTester.write(pagedRequest);

		assertThat(result).extractingJsonPathNumberValue("@.page")
						  .isEqualTo(0);
		assertThat(result).extractingJsonPathNumberValue("@.size")
						  .isEqualTo(1);
	}

	@SpringBootConfiguration
	public static class TestConfig {
	}
}
