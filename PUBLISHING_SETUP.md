# Publishing Setup - Ready for Credentials

Your build is configured and ready for publishing! Here's what you need to do when you're ready to publish to Maven Central.

## Current Status ✅

- [x] GPG key generated and uploaded to key servers
- [x] Build configuration ready in `lib/build.gradle.kts`
- [x] Signing configuration in place
- [x] Maven POM metadata configured
- [x] Repository URLs configured for OSSRH

## What You Need Before Publishing

### 1. Sonatype JIRA Account & Namespace Access

You'll need:

- Sonatype JIRA account at https://issues.sonatype.org/
- Request access to `io.github.menon-codes` namespace
- Get your OSSRH username and password

### 2. Your GPG Key Information

You already have this, but you'll need:

- Your GPG private key (exported with `gpg --export-secret-keys --armor KEYID`)
- Your GPG passphrase

## When Ready to Publish - Add Credentials

### Option 1: Global gradle.properties (RECOMMENDED)

Create/edit `~/.gradle/gradle.properties` (in your user home directory):

```properties
# Maven Central OSSRH credentials
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

# GPG signing credentials
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n\
YOUR_PRIVATE_KEY_HERE_WITH_NEWLINES_ESCAPED\n\
-----END PGP PRIVATE KEY BLOCK-----

signingPassword=your-gpg-passphrase
```

**Note**: The private key should be on a single line with `\n\` at the end of each original line.

### Option 2: Environment Variables

Set these environment variables:

```powershell
$env:ORG_GRADLE_PROJECT_ossrhUsername = "your-sonatype-username"
$env:ORG_GRADLE_PROJECT_ossrhPassword = "your-sonatype-password"
$env:ORG_GRADLE_PROJECT_signingKey = "your-private-key-single-line"
$env:ORG_GRADLE_PROJECT_signingPassword = "your-gpg-passphrase"
```

## Testing Your Setup

### 1. Test Local Publishing (Safe)

```bash
./gradlew publishToMavenLocal
```

This should create `.asc` signature files in `~/.m2/repository/io/github/menon-codes/lib/`

### 2. Test Staging (Safe - doesn't release)

```bash
./gradlew publishAllPublicationsToOSSRHRepository
```

This uploads to Sonatype staging but doesn't release publicly.

### 3. Verify Staged Artifacts

1. Go to https://s01.oss.sonatype.org/
2. Login with your OSSRH credentials
3. Click "Staging Repositories"
4. Find your repository (usually `iogithubmenoncodes-XXXX`)
5. Verify all artifacts are there with signatures

### 4. Release to Maven Central

In the Sonatype web interface:

1. Select your staging repository
2. Click "Close" (validates artifacts)
3. Wait for validation to complete
4. Click "Release" (publishes to Maven Central)

## File Locations for Credentials

### Secure (Recommended)

- `~/.gradle/gradle.properties` - Global, outside project
- Environment variables - For CI/CD

### Never Do

- Don't put credentials in project `gradle.properties`
- Don't commit credentials to git
- Don't put credentials in build files

## Quick Commands When Ready

```bash
# 1. Test everything works locally
./gradlew clean build publishToMavenLocal

# 2. Check for .asc files
ls ~/.m2/repository/io/github/menon-codes/lib/1.0.0-SNAPSHOT/

# 3. Publish to staging
./gradlew publishAllPublicationsToOSSRHRepository

# 4. Check build scan for issues
./gradlew publishAllPublicationsToOSSRHRepository --scan
```

## Troubleshooting

### If signing fails:

- Check GPG key is properly exported and formatted
- Verify GPG passphrase is correct
- Ensure GPG agent is running

### If publishing fails:

- Verify OSSRH credentials are correct
- Check you have access to `io.github.menon-codes` namespace
- Ensure all required POM elements are present

### If validation fails in Sonatype:

- All artifacts must have signatures (.asc files)
- POM must have all required elements
- Sources and javadoc JARs must be present

## Your GPG Key Info

- Key ID: `A9071BC2D6CD1DAC`
- User: `Aditya Menon (MAVEN KOTLIN SECURITY KEY) <adimenon@outlook.com>`
- You've confirmed this key is uploaded to key servers ✅

## Next Steps When Ready

1. Get Sonatype JIRA account and namespace access
2. Add credentials to `~/.gradle/gradle.properties`
3. Test with `./gradlew publishToMavenLocal`
4. Test staging with `./gradlew publishAllPublicationsToOSSRHRepository`
5. Release via Sonatype web interface

Everything is ready - you just need to add the actual credentials when you're ready to publish!
