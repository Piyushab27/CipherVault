# CipherVault: Secure Digital Password Manager

A small offline desktop password manager: Java 17 + Swing + MySQL 8 (JDBC), with password statistics exported to CSV for
analysis in R and Tableau. Built as our SE lab project covering Java, DBMS, SE Lab and DV.

## Run it (quickest, macOS with Homebrew MySQL installed)

```bash
scripts/run.sh
```

This starts a private MySQL on port 3307 (data in `~/.ciphervault-mysql`, root with no password, local only) and then the
app. It does not touch any other MySQL running on port 3306.

## Run it against your own MySQL

You need JDK 17+, Maven and a running MySQL 8.

```bash
mvn package            # compiles, runs the unit tests, copies the connector jar to target/lib
java -jar target/ciphervault.jar
```

The app creates the `ciphervault` database, tables, view, trigger and procedure on first start
(`src/main/resources/schema.sql`). It connects as `root` with no password by default. Change that with environment
variables before starting:

```bash
export CV_DB_USER=root
export CV_DB_PASS=your-mysql-password
# optional: export CV_DB_URL="jdbc:mysql://localhost:3306/ciphervault?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
```

During development `mvn exec:java` also works. Without Maven, put the connector jar on the CLASSPATH:
`java -cp "target/classes:target/lib/mysql-connector-j-8.4.0.jar" com.ciphervault.ui.App`.

## Using it
- **Register** with a username and a master password (must not be Weak: 8+ characters with a digit, an uppercase letter
  and a symbol), then **Login**. Three wrong passwords lock the account; to unlock, run `sql/unlock_user.sql`.
- **Forgot password?** on the login screen: enter your username, answer your security question and choose a new master
  password (this also unlocks the account). Pick the question and answer when you register; older accounts can add one
  with **File > Set security question**. Three wrong answers lock the reset (see `sql/unlock_user.sql`).
  **File > Change master password** changes it while logged in.
- **Dashboard:** categories on the left, entries on the right. Search box (case-insensitive), strength filter,
  **Add**, **Show** (shows the selected password for 30 seconds), **Check Reuse**. Double-click a row to edit; right-click
  for Copy / Edit / Delete (copied passwords leave the clipboard after 30 seconds).
- **File:** Import CSV (`site,username,password,category` with a header row), Export CSV (stats only, no passwords),
  Logout, Exit. **View:** Show passwords, Light / Dark theme.
- The app locks itself after 2 minutes without mouse or keyboard activity.

## Project layout

| Path | What |
|---|---|
| `src/main/java/com/ciphervault/model` | Account, Credential, BankCredential, Searchable, Storable, User, exceptions |
| `.../dao` | DBConnection, UserDAO, CredentialDAO (the only code that runs SQL) |
| `.../service` | VaultService, PasswordGenerator, StrengthChecker, PasswordUtils, Repository&lt;T&gt;, AutoLockThread, PasswordHider, CsvService |
| `.../ui` | App, MainFrame (CardLayout), LoginPanel, DashboardPanel, EntryDialog, Theme |
| `src/main/resources/schema.sql` | tables, index, view, triggers, `add_credential` procedure |
| `src/test/java` | JUnit 5 tests (`mvn test`); `VaultServiceIT` needs MySQL |
| `sql/` | example queries for the DBMS part, unlock script |
| `r/`, `data/`, `docs/TABLEAU.md` | DV: base R, ggplot2, target and control charts on `data/sample_vault_stats.csv` |
| `docs/` | DBMS notes (relational algebra, normalization), SE lab SRS and diagrams |

Run the integration tests against your MySQL with:

```bash
CIPHERVAULT_IT=1 CV_DB_USER=root CV_DB_PASS=your-mysql-password mvn test -Dtest=VaultServiceIT
```

## Notes on the design choices
- Layers: Swing screens call `VaultService`; the service calls the DAOs; only the DAOs touch JDBC.
- The master password is stored as a SHA-256 hash of `username:password`; saved passwords are Base64-encoded in the
  table. That is the syllabus "small extra", not strong encryption: anyone with the database can decode the saved
  passwords. Say so in the report.
- Teammates: add your names to the `README` and the report cover.
