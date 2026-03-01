-- ==========================================
-- 1️⃣ Drop FK from users -> user_role
-- ==========================================
ALTER TABLE users
    DROP CONSTRAINT fk_users_role;

-- ==========================================
-- 2️⃣ Rename table
-- ==========================================
ALTER TABLE user_role
    RENAME TO roles;

-- ==========================================
-- 3️⃣ Rename old PK column (role -> name)
-- ==========================================
ALTER TABLE roles
    RENAME COLUMN role TO name;

-- ==========================================
-- 4️⃣ Add new surrogate ID
-- ==========================================
CREATE SEQUENCE roles_id_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE roles
    ADD COLUMN id BIGINT;

UPDATE roles
SET id = nextval('roles_id_seq');

-- Make id NOT NULL
ALTER TABLE roles
    ALTER COLUMN id SET NOT NULL;

-- ==========================================
-- 5️⃣ Drop old PK (name) and set new PK (id)
-- ==========================================
ALTER TABLE roles
    DROP CONSTRAINT user_role_pkey;

ALTER TABLE roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

-- Add unique constraint to name
ALTER TABLE roles
    ADD CONSTRAINT uk_roles_name UNIQUE (name);

-- ==========================================
-- 6️⃣ Add temp column to users for migration
-- ==========================================
ALTER TABLE users
    ADD COLUMN role_tmp BIGINT;

-- Map old role_id (string) -> new roles.id
UPDATE users u
SET role_tmp = r.id
FROM roles r
WHERE u.role_id = r.name;

-- ==========================================
-- 7️⃣ Replace old role_id column
-- ==========================================
ALTER TABLE users
    DROP COLUMN role_id;

ALTER TABLE users
    RENAME COLUMN role_tmp TO role_id;

ALTER TABLE users
    ALTER COLUMN role_id SET NOT NULL;

-- ==========================================
-- 8️⃣ Recreate foreign key
-- ==========================================
ALTER TABLE users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id);

-- ==========================================
-- 9️⃣ Rename auditing FK constraints
-- ==========================================
ALTER TABLE roles
    RENAME CONSTRAINT fk_user_role_created_by TO fk_roles_created_by;

ALTER TABLE roles
    RENAME CONSTRAINT fk_user_role_last_modified_by TO fk_roles_last_modified_by;

-- ==========================================
-- 🔟 Add description column (optional)
-- ==========================================
ALTER TABLE roles
    ADD COLUMN description VARCHAR(500);