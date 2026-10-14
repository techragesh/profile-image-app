# profile-image-app

Spring Boot + Thymeleaf demo: upload a profile picture to Azure Blob
Storage, store its URL in Azure SQL, and display it as a debit-card-style
"DP" card. No CDN - images are streamed through this app.

## 1. One-time Azure setup

- Storage account `proficpic` (resource group `rg_ab_poc`, region
  `francecentral`), container `profiles` already exists per your details.
- Grant this app's identity the **Storage Blob Data Contributor** role on
  the storage account (Azure Portal -> storage account -> Access Control
  (IAM) -> Add role assignment). Locally, `az login` lets
  `DefaultAzureCredential` pick up your own identity for testing.
- SQL server `profilepic.database.windows.net`, database
  `profile-sql-db-8000263`. Make sure its firewall allows your IP (or
  "Allow Azure services" if this app runs inside Azure).

## 2. Create the table

Run this once against `profile-sql-db-8000263`:

```sql
CREATE TABLE ProfileImageRef (
    user_id       BIGINT PRIMARY KEY,
    image_url     NVARCHAR(500) NOT NULL,
    version       INT NOT NULL DEFAULT 1,
    content_type  NVARCHAR(50) NOT NULL,
    updated_at    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ProfileImageRef_TableA
        FOREIGN KEY (user_id) REFERENCES TableA(user_id)
);
```

(Drop the `FOREIGN KEY` constraint if `TableA` doesn't exist yet in this
environment.)

## 3. Set the DB password as an environment variable

Never commit the real password. Set it before running:

```bash
export DB_PASSWORD=your_real_password      # macOS/Linux
$env:DB_PASSWORD="your_real_password"      # Windows PowerShell
```

`admin`/`admin` is a weak, guessable pair - rotate it before this goes
anywhere beyond a local POC.

## 4. Run it

```bash
mvn spring-boot:run
```

Then open: `http://localhost:8080/profile/1` (any numeric user id works)
to see the card and upload a picture.

## Project layout

```
pom.xml
src/main/java/com/example/profile/
  ProfileApplication.java
  entity/ProfileImageRef.java
  repository/ProfileImageRefRepository.java
  service/AzureBlobStorageService.java
  service/ProfileImageService.java
  controller/ProfileWebController.java
src/main/resources/
  application.yml
  templates/profile.html
  static/css/card.css
```

## Notes

- `image_url` stores the full blob URL; the URL carries no credentials,
  so storing it is safe as long as the container stays private and reads
  go through this app (Managed Identity).
- No CDN: `/profile/{userId}/image` streams bytes from Blob Storage on
  every request. Fine for low/medium traffic. If reads grow, add Azure
  CDN in front of the container and point the `<img>` at the CDN URL
  instead - the upload path and DB schema don't need to change.
- For production, consider switching the DB connection to Azure AD /
  Managed Identity auth too (`authentication=ActiveDirectoryManagedIdentity`
  on the JDBC URL), removing the password entirely.
