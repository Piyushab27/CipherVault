# DBMS notes

## ER diagram (draw in StarUML or draw.io)
- **User** (user_id PK, username, master_hash, failed_attempts) 1 ──< **Credential** (cred_id PK, user_id FK, category_id FK, site_name, username, password, strength, updated_at)
- **Category** (category_id PK, name) 1 ──< **Credential**

One user has many credentials; each credential belongs to one category (a user-to-category link goes through credentials).

## Relational algebra
1. Weak sites: π site_name (σ strength='Weak' (credentials))
2. Sites with their category: π site_name, name (credentials ⋈ categories)
3. Banking entries of user 1: π site_name, username (σ user_id=1 ∧ name='Banking' (credentials ⋈ categories))
4. Users with no credentials: π user_id (users) − π user_id (credentials)

## Normalization (one big table to 3NF)
Start: `vault(username, master_hash, failed_attempts, site_name, site_username, password, strength, updated_at, category_name)`

- **1NF**: every cell holds one value (no list of passwords in one column). Key: (username, site_name, site_username).
- **2NF**: `master_hash` and `failed_attempts` depend only on `username`, not on the whole key → move to `users`.
- **3NF**: `category_name` is repeated for many rows and is a fact about the category, not about the credential → move to `categories`; credentials keep `category_id`.

Result: `users`, `categories`, `credentials` (see `src/main/resources/schema.sql`).

## ACID in the edit transaction
`CredentialDAO.update` turns auto-commit off, runs the UPDATE, then commits; any `SQLException` triggers `rollback()`.
Atomicity: the update happens fully or not at all. Consistency: foreign keys and NOT NULL still hold. Isolation: other
sessions do not see the half-done change. Durability: after commit it survives a restart (test TC_IT_02).

## Index and B+ tree
`idx_site_name` on `credentials(site_name)` is a B+ tree: keys stay sorted in the leaves, which are linked, so a prefix
search such as `LIKE 'Insta%'` walks down a few levels and reads neighbouring leaves instead of scanning the table.
