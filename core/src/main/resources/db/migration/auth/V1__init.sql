-- Begin with the creation of the database schema for the authentication and access control list (ACL) handling
CREATE TABLE user_account
(
	id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id         BIGINT       NOT NULL UNIQUE,
	locked         BOOLEAN      NOT NULL DEFAULT false,
	birthday       TIMESTAMP    NOT NULL,
	description    VARCHAR(255),
	email          VARCHAR(255) NOT NULL UNIQUE,
	login_name     VARCHAR(255) NOT NULL UNIQUE,
	name           VARCHAR(255) NOT NULL,
	nick           VARCHAR(255) NOT NULL,
	password       VARCHAR(255) NOT NULL,
	priv_type      VARCHAR(10)  NOT NULL CHECK (priv_type IN ('PRIVATE', 'GROUP', 'PUBLIC')),
	public_account BOOLEAN      NOT NULL DEFAULT false,
	street         VARCHAR(255)          DEFAULT NULL,
	pob            VARCHAR(255)          DEFAULT NULL,
	status         VARCHAR(10)  NOT NULL CHECK (status IN ('REGISTERED', 'ACTIVE', 'DISABLED')),
	creator        BIGINT       NOT NULL,
	created        TIMESTAMP    NOT NULL,
	modifier       BIGINT,
	modified       TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE acl
(
	id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id           BIGINT  NOT NULL,
	create_privilege BOOLEAN NOT NULL,
	delete_privilege BOOLEAN NOT NULL,
	modify_privilege BOOLEAN NOT NULL,
	read_privilege   BOOLEAN NOT NULL,
	unit_id          BIGINT,
	user_id          BIGINT
);

-- Only either unit_id or user_id is not null
ALTER TABLE acl
	ADD CONSTRAINT acl_unit_user_xor CHECK ((unit_id IS NOT NULL AND user_id IS NULL) OR (unit_id IS NULL AND user_id IS NOT NULL));

-- There can be no two rows with the same acl_id, user_id or unit_id
ALTER TABLE acl
	ADD CONSTRAINT acl_unique_acl_id_user_id_unit_id UNIQUE (acl_id, user_id, unit_id);

-- Create a dedicated sequence for acl.acl_id and initialize it to max(acl_id)+1 to avoid collisions
CREATE SEQUENCE IF NOT EXISTS acl_acl_id_seq AS BIGINT;

-- Set the sequence next value to max(acl_id)+1 so that nextval will return a safe, non-colliding value
SELECT setval('acl_acl_id_seq', COALESCE((SELECT MAX(acl_id) FROM acl), 0) + 1, false);

-- Optionally set default for acl.acl_id to use the sequence for future inserts
ALTER TABLE acl
	ALTER COLUMN acl_id SET DEFAULT nextval('acl_acl_id_seq');

CREATE TABLE unit
(
	id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	acl_id      BIGINT       NOT NULL UNIQUE,
	description VARCHAR(255),
	name        VARCHAR(255) NOT NULL UNIQUE,
	locked      BOOLEAN      NOT NULL DEFAULT false,
	creator     BIGINT       NOT NULL,
	created     TIMESTAMP    NOT NULL,
	modifier    BIGINT,
	modified    TIMESTAMP,
	FOREIGN KEY (creator) REFERENCES user_account (id),
	FOREIGN KEY (modifier) REFERENCES user_account (id)
);

CREATE TABLE user_unit
(
	id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	user_id BIGINT NOT NULL,
	unit_id BIGINT NOT NULL,
	FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
	FOREIGN KEY (unit_id) REFERENCES unit (id) ON DELETE CASCADE
);

-- End of authentication ACL handling

-- Create sequence used by the hi/lo ACL ID generator
CREATE SEQUENCE IF NOT EXISTS acl_hi_seq;

