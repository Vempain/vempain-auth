package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * In-memory paging of the small user and unit lists: filters by the request's search text, sorts by name (or id) and cuts the page.
 */
final class PagingTools {
	private PagingTools() {
	}

	/**
	 * @param rows       every row
	 * @param searchable the texts of a row the search is matched against (name, login name, ...)
	 * @param name       the default sort key
	 * @param id         the sort key used when {@code sort_by} is {@code id}
	 */
	static <T> PagedResponse<T> page(List<T> rows, PagedRequest request, Function<T, List<String>> searchable, Function<T, String> name,
									 Function<T, Long> id) {
		var responses     = new ArrayList<>(rows);
		var query         = request.getSearch();
		var caseSensitive = Boolean.TRUE.equals(request.getCaseSensitive());

		if (query != null && !query.isBlank()) {
			var               normalized = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
			Predicate<String> matches    = text -> text != null && (caseSensitive ? text : text.toLowerCase(Locale.ROOT)).contains(normalized);
			responses.removeIf(row -> searchable.apply(row)
												.stream()
												.noneMatch(matches));
		}

		responses.sort(Comparator.comparing(name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		if ("id".equals(request.getSortBy())) {
			responses.sort(Comparator.comparing(id));
		}
		if (request.getDirection() == Sort.Direction.DESC) {
			Collections.reverse(responses);
		}

		int size  = Math.max(1, request.getSize());
		int pages = (int) Math.ceil((double) responses.size() / size);
		long from = Math.min((long) Math.max(0, request.getPage()) * size, responses.size());
		long to   = Math.min(from + size, responses.size());
		return PagedResponse.of(responses.subList((int) from, (int) to), request.getPage(), size, responses.size(), pages,
								request.getPage() == 0, pages == 0 || (long) request.getPage() >= (long) pages - 1);
	}
}
