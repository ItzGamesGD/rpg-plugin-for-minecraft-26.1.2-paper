# Security Policy

## Supported versions

HyunseoRPG is a development-frozen reference plugin. Security fixes may be applied to the latest repository state, but older snapshots and releases are not guaranteed to receive updates.

## Reporting a vulnerability

Please **do not open a public issue** for a vulnerability that could expose credentials, private server data, player data, or enable abuse of a running server.

If GitHub Private Vulnerability Reporting is available for this repository, use that channel. Otherwise, contact the repository maintainer through the maintainer's GitHub profile before publishing technical details.

When reporting a vulnerability, include only the minimum information necessary to reproduce it. Remove or redact:

- access tokens and API keys
- server IP addresses that are intended to remain private
- player UUID mappings or personal data
- database contents
- private configuration files
- world or server backups containing unrelated data

## Secrets

The repository should never contain production credentials. Use local environment configuration or GitHub's encrypted secret storage for any future automation that requires credentials.

If a secret is accidentally committed, deleting the file in a later commit is not sufficient. Revoke or rotate the secret immediately and then clean the Git history if necessary.
