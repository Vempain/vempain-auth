package fi.poltsi.vempain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AclIdGenerator {
	private final    JdbcTemplate jdbcTemplate;
	private final    int          blockSize;
	private final    AtomicLong   counter     = new AtomicLong(0L);
	// current block base (inclusive) and end (inclusive)
	private volatile long         currentBase = 0L;
	private volatile long         currentMax  = -1L;

	public AclIdGenerator(JdbcTemplate jdbcTemplate, @Value("${vempain.acl.block-size:1000}") int blockSize) {
		this.jdbcTemplate = jdbcTemplate;
		this.blockSize    = blockSize;
	}

	public long nextAclId() {
		while (true) {
			var id   = counter.incrementAndGet();
			var base = currentBase;
			var max  = currentMax;

			if (id <= (max - base + 1)) {
				return base + id - 1;
			}

			// need to refill block
			synchronized (this) {
				// double-check after acquiring lock
				var used = counter.get();

				if (used <= (currentMax - currentBase + 1)) {
					var allocatedIndex = counter.incrementAndGet();
					return currentBase + allocatedIndex - 1;
				}

				// Reserve next hi from DB sequence
				var hi = jdbcTemplate.queryForObject("SELECT nextval('acl_hi_seq')", Long.class);
				// compute new base and max
				var newBase = (hi - 1) * (long) blockSize + 1L;
				var newMax  = hi * (long) blockSize;

				this.currentBase = newBase;
				this.currentMax  = newMax;
				this.counter.set(0L);

				// allocate first id from new block
				var allocatedIndex = counter.incrementAndGet();
				return currentBase + allocatedIndex - 1;
			}
		}
	}
}

