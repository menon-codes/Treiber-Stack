# Publishing Commands Helper

# Quick commands for when you're ready to publish

## Test Signing Setup (Safe - Local Only)

```bash
# This should create .asc signature files if signing is working
./gradlew clean publishToMavenLocal

# Check for signature files
ls ~/.m2/repository/io/github/menon-codes/lib/1.0.0-SNAPSHOT/
# You should see: .jar.asc, .pom.asc, -sources.jar.asc files
```

## Test Staging (Safe - Uploads to Sonatype but doesn't release)

```bash
# Upload to Sonatype staging repository
./gradlew publishAllPublicationsToOSSRHRepository

# If successful, check at https://s01.oss.sonatype.org/
# Login → Staging Repositories → Look for iogithubmenoncodes-XXXX
```

## Release Process (Final Step)

1. Go to https://s01.oss.sonatype.org/
2. Login with OSSRH credentials
3. Find your staging repository
4. Click "Close" (validates artifacts)
5. Wait for validation
6. Click "Release" (publishes to Maven Central)

## Troubleshooting Commands

```bash
# Build with full logging
./gradlew publishAllPublicationsToOSSRHRepository --info --stacktrace

# Generate build scan for debugging
./gradlew publishAllPublicationsToOSSRHRepository --scan

# Check what artifacts will be published
./gradlew publishToMavenLocal --dry-run

# Verify GPG setup
gpg --list-secret-keys --keyid-format=long
gpg --export --armor A9071BC2D6CD1DAC
```

## Environment Variables Alternative

If you prefer environment variables over global gradle.properties:

### PowerShell (Windows)

```powershell
$env:ORG_GRADLE_PROJECT_ossrhUsername = "your-username"
$env:ORG_GRADLE_PROJECT_ossrhPassword = "your-password"
$env:ORG_GRADLE_PROJECT_signingKey = "your-key-single-line"
$env:ORG_GRADLE_PROJECT_signingPassword = "your-passphrase"

# Then run commands
./gradlew publishAllPublicationsToOSSRHRepository
```

### Bash (Linux/Mac)

```bash
export ORG_GRADLE_PROJECT_ossrhUsername="your-username"
export ORG_GRADLE_PROJECT_ossrhPassword="your-password"
export ORG_GRADLE_PROJECT_signingKey="your-key-single-line"
export ORG_GRADLE_PROJECT_signingPassword="your-passphrase"

./gradlew publishAllPublicationsToOSSRHRepository
```

## What to Expect

- First time publishing: May take a few hours for Maven Central sync
- Subsequent releases: Usually available in Maven Central within 30 minutes
- Staging validation: Should complete within a few minutes
- Build time: ~1-2 minutes for signing and upload
