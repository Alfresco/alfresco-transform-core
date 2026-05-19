# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build with Docker images
mvn clean install -Plocal,docker-it-setup

# Build and test a specific engine
mvn clean verify -Plocal,docker-it-setup,imagemagick
mvn clean verify -Plocal,docker-it-setup,libreoffice
mvn clean verify -Plocal,docker-it-setup,tika
mvn clean verify -Plocal,docker-it-setup,aio-test

# Skip tests
mvn clean install -DskipTests

# Run a specific test class
mvn test -Dtest=TestClassName

# Run a specific test method
mvn test -Dtest=TestClassName#testMethodName

# Code formatting
mvn spotless:apply
mvn spotless:check
```

Available build profiles: `full-build` (default), `base`, `imagemagick`, `libreoffice`, `misc`, `pdf-renderer`, `tika`, `aio-test`, `example`. All profiles include `model` and `base` modules.

## Architecture

This project is a framework for building **T-Engines** (Transform Engines) — Spring Boot microservices that perform content transformations (PDF→image, Office→PDF, metadata extraction, etc.) for Alfresco Content Services.

### Module layout

- **`model/`** — Shared data model JAR (`alfresco-transform-model`). Contains config objects (`TransformConfig`, `Transformer`, `SupportedSourceAndTarget`), registry, messaging DTOs, and MIME type utilities. Must be in `org.alfresco.transform` package.
- **`engines/base/`** — Base Spring Boot JAR (`alfresco-base-t-engine`). Provides the REST API, JMS messaging, health probes, and the three core interfaces that concrete engines implement.
- **`engines/{imagemagick,libreoffice,misc,pdfrenderer,tika}/`** — Concrete T-Engine Docker images, each implementing the interfaces from `base`.
- **`engines/aio/`** — All-In-One T-Engine combining all the above engines into a single Docker image.
- **`engines/example/`** — Reference implementation for building a custom T-Engine.
- **`deprecated/alfresco-transformer-base/`** — Original base module, superseded by `engines/base/`.

### Core interfaces (all in `engines/base`)

**`TransformEngine`** — Declares engine metadata:
- `getTransformEngineName()` — unique sorted-alphanumeric name
- `getTransformConfig()` — returns the JSON config describing supported transforms
- `getProbeTransform()` — K8s readiness/liveness probe definition

**`CustomTransformer`** — Implements transform logic:
- `getTransformerName()` — must match the `transformerName` in the JSON config
- `transform(sourceMimetype, inputStream, targetMimetype, outputStream, transformOptions, transformManager)`

**`TransformManager`** — Engine API available during transform:
- `createSourceFile()` / `createTargetFile()` — get local temp files instead of streams
- `respondWithFragment(index, finished)` — support multi-part responses

Implementations must be Spring `@Component` beans in the `org.alfresco.transform` package for auto-discovery.

### Transform configuration

Each engine ships a JSON config file (e.g., `pdfrenderer_engine_config.json`) that declares: supported source/target MIME types and size limits, transform options/groups, transformer names, and pipeline steps. The base engine combines configs from all discovered `TransformEngine` beans into a registry at startup. The config is exposed at `GET /transform/config`.

### Request flow

Transforms arrive either via **HTTP** (`POST /transform`) or **JMS** (ActiveMQ queue, configured via `TRANSFORM_ENGINE_REQUEST_QUEUE`). The base engine handles routing, auth, probe gating, and temp-file lifecycle — the `CustomTransformer` only sees clean input/output streams.

## Running a T-Engine locally

```bash
java -jar engines/pdfrenderer/target/alfresco-pdf-renderer-engine-*.jar
# Test UI: http://localhost:8090/
# Config:  http://localhost:8090/transform/config
```

Required env vars vary by engine (e.g., `IMAGEMAGICK_EXE`, `LIBREOFFICE_HOME`, `PDF_RENDERER_EXE`). See each engine's `application-default.yaml`.

## Key documentation

- `docs/transform-config.md` — JSON config format reference
- `docs/transform-specific-code.md` — How to build a custom T-Engine
- `engines/base/README.md` — Detailed base engine guide with code examples
- `docs/t-engines.md` — T-Engine overview
- `docs/Probes.md` — K8s health probe details
