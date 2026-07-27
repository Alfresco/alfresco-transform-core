# Build
The `alfresco-transform-core` project uses _GitHub Actions CI_. \
The `ci.yml` config file can be found in the `.github/workflows` directory of the project.


## Stages and Jobs
1. **Build**: Java build with unit and integration tests.
2. **Release**: Release with artifact deployment to Nexus, DockerHub and Quay.io.

> The _Release_ stage uses the
> [`maven-release-slim`](https://github.com/Alfresco/alfresco-build-tools/tree/master/.github/actions/maven-release-slim)
> action from `alfresco-build-tools`. It authenticates with a **GitHub App token**, produces
> **verified (signed) commits and tags**, and deploys artifacts with `mvn deploy` (no
> `maven-release-plugin`). The release and next development versions are provided explicitly
> (see the _Release process steps_ below), which avoids the SemVer auto-increment issue.


## Branches
GitHub Actions CI builds differ by branch:
* `master` / `SP/*` / `HF/*` branches:
  - regular builds which include the _Build_ stage;
    > On the `master` branch only the _Build_ stage updates the `latest` T-Engines images on 
    > both Quay and DockerHub:
    > - alfresco/alfresco-pdf-renderer
    > - alfresco/alfresco-imagemagick
    > - alfresco/alfresco-tika
    > - alfresco/alfresco-libreoffice
    > - alfresco/alfresco-transform-misc
    > - alfresco/alfresco-transform-core-aio
  - if the commit message contains the `[release]` tag, the builds will also 
  include the _Release_ stage;
* `ATS-*` / `ACS-*` branches:
  - regular builds which include only the _Build_ and _Tests_ stages;

All other branches are ignored.


## Release process steps & info
Prerequisites:
 - the `master` / `SP/*` / `HF/*` branch is green and it contains all the changes that should be
 included in the next release.
 - the repository has the GitHub App configured for verified releases: the
 `GH_APP_ENGINEERING_CONTRIB_CLIENT_ID` variable and `GH_APP_ENGINEERING_CONTRIB_PRIVATE_KEY`
 secret are available, and the App is installed with `contents: write` permission.

Steps:
1. Create a new branch with the name `ATS-###_release_version` from the `master` / `SP/*`/ `HF/*`
branch.
2. Set the release and next development versions in the `env` block of
`.github/workflows/ci.yml`:
    ```yaml
    RELEASE_VERSION: "5.4.5-A.1"                 # the version of the release (git tag)
    DEVELOPMENT_VERSION: "5.4.5-A.2-SNAPSHOT"    # the version set in the POMs after the release
    ```
    > These replace the previous `mvn versions:set` step. The `maven-release-slim` action sets
    > `RELEASE_VERSION` in every `pom.xml`, deploys the artifacts, creates the verified tag, then
    > sets `DEVELOPMENT_VERSION` for the next iteration - all as verified commits.
3. Create a new commit with the `[release]` tag in its message. The version changes from step (2)
can be included in this same commit - e.g.
     ```bash
     git commit -am "ATS-###: Release T-Core (T-Engines) 5.4.5-A.1 [release]"
     ```

     > The location of the `[release]` tag in the commit message is irrelevant.

4. Open a new Pull Request from the `ATS-###_release_version` branch into the original
`master` / `SP/*` / `HF/*` branch and wait for a green build.
5. Once it is approved, merge the PR, preferably through the **Rebase and merge** option. If the
**Create a merge commit** (_Merge pull request_) or **Squash and merge** options are used, you
need to ensure that the _commit message_ contains the `[release]` tag (sub-string).
6. After the _Release_ stage completes, verify in GitHub that the release commits and the new tag
are marked as **Verified**.


