package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagingToolsUTC {
	@Test
	void maximumPageReturnsAnEmptyLastPageWithoutOverflow() {
		var request = PagedRequest.builder()
								  .page(Integer.MAX_VALUE)
								  .size(2)
								  .build();

		var response = PagingTools.page(List.of("a", "b", "c"), request, List::of, value -> value, value -> 0L);

		assertEquals(List.of(), response.getContent());
		assertEquals(Integer.MAX_VALUE, response.getPage());
		assertFalse(response.isFirst());
		assertTrue(response.isLast());
	}
}
