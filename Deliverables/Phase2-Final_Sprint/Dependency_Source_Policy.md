# Dependency Source Policy

## Purpose

This policy defines the approved sources for third-party components and the controls used to reduce dependency confusion risk.

## Approved Repositories

The application uses Maven for Java dependencies and build plugins. The only approved external repository for Maven dependencies and plugins is Maven Central.

The project does not currently use private or internally published Maven packages. If private packages are introduced later, they must use an internal group ID namespace controlled by the team and must resolve only from an approved internal repository or repository proxy.

## Current Repository Configuration

The project `pom.xml` does not define custom `<repositories>` or `<pluginRepositories>` entries. Maven therefore resolves public dependencies and plugins from the configured default repository, Maven Central.

No ad-hoc repositories such as JitPack, personal package feeds, temporary vendor repositories, or snapshot repositories may be added without review and documentation.

## Dependency Confusion Controls

To reduce dependency confusion risk:

- New dependencies and plugins must be reviewed before merge.
- New repository declarations require security review and must be documented in this file.
- Internal package names must not overlap with public package coordinates.
- Private dependencies, if added, must be resolved from an internal repository or controlled mirror before any public repository is considered.
- Snapshot and dynamic dependency versions are not allowed for production builds.
- CI dependency checks must run against the resolved dependency tree.

## Verification

For this repository, verification consists of reviewing `pom.xml` and confirming:

- no custom `<repositories>` entries are present;
- no custom `<pluginRepositories>` entries are present;
- dependencies use expected public group IDs and artifact IDs;
- dependency vulnerability scanning is run in CI;
- any future repository additions are documented and reviewed.

This policy must be reviewed when dependencies, Maven plugins, build tooling, or repository configuration change.
