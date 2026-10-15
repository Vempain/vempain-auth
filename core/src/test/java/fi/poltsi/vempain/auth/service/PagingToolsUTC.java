package fi.poltsi.vempain.auth.service;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;

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

    private record Row(long id, String name, String login) {}

    // Deliberately unsorted, with a row without a name and a row without a login name
    private static final List<Row> ROWS = List.of(new Row(3L, "Charlie", "charlie"), new Row(1L, "alice", "ali"), new Row(2L, "Bob", null),
            new Row(4L, null, "anon"));

    private static PagedResponse<Row> page(PagedRequest request) {
        return PagingTools.page(ROWS, request, row -> Arrays.asList(row.name(), row.login()), Row::name, Row::id);
    }

    private static List<Long> ids(PagedResponse<Row> response) {
        return response.getContent()
            .stream()
            .map(Row::id)
            .toList();
    }

    @Test
    void rowsAreSortedByNameCaseInsensitivelyWithMissingNamesLast() {
        var response = page(PagedRequest.builder()
            .page(0)
            .size(10)
            .build());

        assertEquals(List.of(1L, 2L, 3L, 4L), ids(response));
        assertEquals(4, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void sortByIdAndDescendingDirectionAreHonoured() {
        assertEquals(List.of(1L, 2L, 3L, 4L), ids(page(PagedRequest.builder()
            .size(10)
            .sortBy("id")
            .build())));
        assertEquals(List.of(4L, 3L, 2L, 1L), ids(page(PagedRequest.builder()
            .size(10)
            .sortBy("id")
            .direction(Sort.Direction.DESC)
            .build())));
        // Descending by name puts the nameless row first
        assertEquals(List.of(4L, 3L, 2L, 1L), ids(page(PagedRequest.builder()
            .size(10)
            .direction(Sort.Direction.DESC)
            .build())));
    }

    @Test
    void searchIsCaseInsensitiveByDefaultAndMatchesAnySearchableText() {
        // "AL" matches the name "alice" and its login "ali"; "ANON" only the login of the nameless row
        assertEquals(List.of(1L), ids(page(PagedRequest.builder()
            .size(10)
            .search("AL")
            .build())));
        assertEquals(List.of(4L), ids(page(PagedRequest.builder()
            .size(10)
            .search("ANON")
            .build())));
        assertEquals(List.of(), ids(page(PagedRequest.builder()
            .size(10)
            .search("zzz")
            .build())));
    }

    @Test
    void caseSensitiveSearchMatchesTheExactCase() {
        assertEquals(List.of(), ids(page(PagedRequest.builder()
            .size(10)
            .search("AL")
            .caseSensitive(true)
            .build())));
        assertEquals(List.of(1L), ids(page(PagedRequest.builder()
            .size(10)
            .search("al")
            .caseSensitive(true)
            .build())));
    }

    @Test
    void blankSearchKeepsEveryRow() {
        assertEquals(4, page(PagedRequest.builder()
            .size(10)
            .search("  ")
            .build()).getTotalElements());
    }

    @Test
    void pagesCarryTheFirstAndLastFlags() {
        var first = page(PagedRequest.builder()
            .page(0)
            .size(3)
            .build());
        assertEquals(List.of(1L, 2L, 3L), ids(first));
        assertEquals(2, first.getTotalPages());
        assertTrue(first.isFirst());
        assertFalse(first.isLast());

        var last = page(PagedRequest.builder()
            .page(1)
            .size(3)
            .build());
        assertEquals(List.of(4L), ids(last));
        assertFalse(last.isFirst());
        assertTrue(last.isLast());
    }

    @Test
    void emptyListYieldsASingleEmptyPage() {
        var response = PagingTools.page(List.<Row>of(), PagedRequest.builder()
            .size(5)
            .build(), row -> List.of(row.name()), Row::name, Row::id);

        assertEquals(List.of(), response.getContent());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void nonPositiveSizeFallsBackToOneRowPerPage() {
        var response = page(PagedRequest.builder()
            .page(0)
            .size(0)
            .build());

        assertEquals(List.of(1L), ids(response));
        assertEquals(1, response.getSize());
        assertEquals(4, response.getTotalPages());
    }
}
