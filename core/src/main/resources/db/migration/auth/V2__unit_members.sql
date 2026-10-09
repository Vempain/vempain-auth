-- Nested units: a unit may contain other units (and, through user_unit, users). A unit never contains itself, directly or through a
-- chain of units; the application rejects such memberships before they are stored.
CREATE TABLE unit_unit
(
	id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	parent_unit_id BIGINT NOT NULL,
	child_unit_id  BIGINT NOT NULL,
	CONSTRAINT unit_unit_not_self CHECK (parent_unit_id <> child_unit_id),
	CONSTRAINT unit_unit_unique UNIQUE (parent_unit_id, child_unit_id),
	FOREIGN KEY (parent_unit_id) REFERENCES unit (id) ON DELETE CASCADE,
	FOREIGN KEY (child_unit_id) REFERENCES unit (id) ON DELETE CASCADE
);

CREATE INDEX unit_unit_child_idx ON unit_unit (child_unit_id);
