INSERT INTO user_account (id, acl_id, birthday, created, creator, locked, email, login_name, name, nick, password, priv_type, public_account, street, status)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, '1900-01-01 00:00:00', NOW(), 1, false, 'admin@nohost.nodomain', 'admin', 'Vempain Administrator', 'Admin', 'Disabled', 'PRIVATE', false, '',
		'ACTIVE');

SELECT setval('user_account_id_seq', (SELECT MAX(id) + 1 FROM user_account));

INSERT INTO acl (id, acl_id, create_privilege, delete_privilege, modify_privilege, read_privilege, unit_id, user_id)
	OVERRIDING SYSTEM VALUE
VALUES (1, 1, true, true, true, true, NULL, 1),
	   (2, 2, true, true, true, true, NULL, 1),
	   (3, 3, true, true, true, true, NULL, 1),
	   (4, 4, true, true, true, true, NULL, 1);

SELECT setval('acl_id_seq', (SELECT MAX(id) + 1 FROM acl));

INSERT INTO unit (id, acl_id, created, creator, locked, modified, modifier, description, name)
	OVERRIDING SYSTEM VALUE
VALUES (1, 2, NOW(), 1, false, null, null, 'Admin group', 'Admin'),
	   (2, 3, NOW(), 1, false, null, null, 'Poweruser group', 'Poweruser'),
	   (3, 4, NOW(), 1, false, null, null, 'Editor group', 'Editor');

SELECT setval('unit_id_seq', (SELECT MAX(id) + 1 FROM unit));
SELECT setval('acl_acl_id_seq', (SELECT MAX(acl_id) + 1 FROM acl));
