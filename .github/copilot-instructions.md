# Copilot / AI assistant instructions — Alfresco Transform Core

Guidance for AI coding agents working in this repository. Keep changes minimal,
consistent with existing conventions, and always buildable.

## What this project is

Alfresco Transform Core contains the common transformer ("T-Engine") code plus a set of
concrete T-Engine implementations. Each T-Engine is a Spring Boot application that is also
packaged as a Docker image. Client, T-Engine and T-Router exchange JSON described by the
shared `model` library.

## Tech stack

- **Java 17** (`java.version` in the root `pom.xml`); some artifacts also target Java 11
  (`acs-compatible.java.version`) for ACS compatibility.
- **Spring Boot** (inherited from `spring-boot-starter-parent`).
- **Maven** multi-module build. **Docker** images per engine.
- Code style enforced via **Spotless** and license-header checks; hooks run through
  **pre-commit**.

## Repository layout

- `model/` — JSON data model + transform-selection logic, packaged as a jar.
- `engines/base/` — code common to all T-Engines (the current base).
- `engines/<name>/` — individual T-Engines (`imagemagick`, `libreoffice`, `misc`,
  `pdfrenderer`, `tika`, `example`) plus `aio` (All-In-One). Each builds a Spring Boot jar
  and a Docker image.
- `deprecated/alfresco-transformer-base/` — the original base; retained but superseded by
  `engines/base`.
- `_ci/` — CI helper scripts (`build.sh`, `test.sh`, `cache_artifacts.sh`).
- `docs/` — additional documentation (transform config, probes, scaling, release, …).
- `scripts/hooks/` — local pre-commit hook scripts (formatting + license headers).

Maven profiles select which modules build: `full-build` (default, everything), `base`,
and one per engine (e.g. `imagemagick`, `libreoffice`, `misc`, `pdf-renderer`, `tika`).

## Build & test

- Full local build with per-engine Docker images and integration setup:

  ```bash
  mvn clean install -Plocal,docker-it-setup
  ```

- Base libraries only: `mvn clean install -Pbase`.
- A single engine locally, mirroring CI: `bash _ci/build.sh <buildProfile>` then
  `bash _ci/test.sh <testProfile>` (see the matrix in `.github/workflows/ci.yml`).
- Integration tests are `*IT.java` and require the `docker-it-setup` profile.

## Running a T-Engine

A T-Engine is a Spring Boot app (`org.alfresco.transform.base.Application`):

- `mvn spring-boot:run`, or `java -jar target/<engine>-{version}.jar`.
- Serves on port `8090`; test page at `http://localhost:8090/`, config at
  `http://localhost:8090/transform/config`.

## Conventions

- Keep formatting Spotless-clean and preserve license headers — the
  `scripts/hooks/check-format-and-headers.sh` pre-commit hook fixes Java files.
- Pin dependency and plugin versions via `<properties>` in the root `pom.xml` rather than
  inline in child modules.
- In GitHub Actions, third-party actions must be SHA-pinned with a version comment; only
  `Alfresco/alfresco-build-tools/*` may use release tags. Never place secrets in a
  workflow-level `env` block — scope them to the steps that need them.

## Where to look first

- Root `pom.xml` for versions, profiles and module wiring.
- `README.md` for a high-level overview and artifact/Docker details.
- `docs/` for transform config, transformer selection, probes and the release process.
