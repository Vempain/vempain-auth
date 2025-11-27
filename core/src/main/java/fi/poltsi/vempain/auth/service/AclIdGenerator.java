package fi.poltsi.vempain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AclIdGenerator {
	private final    JdbcTemplate jdbcTemplate;
	private final    int          blockSize;
	private final    AtomicLong nextId      = new AtomicLong(1L);
	private volatile long       currentBase = 0L;
	private volatile long       currentMax  = 0L;

	public AclIdGenerator(JdbcTemplate jdbcTemplate, @Value("${vempain.acl.block-size:1000}") int blockSize) {
		this.jdbcTemplate = jdbcTemplate;
		this.blockSize    = blockSize;
	}

	public long nextAclId() {
		while (true) {
			var candidate = nextId.getAndIncrement();
			var max       = currentMax;

			if (candidate <= max) {
				return candidate;
			}

			synchronized (this) {
				if (candidate <= currentMax) {
					continue;
				}

				var hi      = jdbcTemplate.queryForObject("SELECT nextval('acl_hi_seq')", Long.class);
				var newBase = (hi - 1L) * blockSize + 1L;
				var newMax  = hi * blockSize;

				currentBase = newBase;
				currentMax  = newMax;
				nextId.set(newBase + 1L);

				return newBase;
			}
		}
	}
}
