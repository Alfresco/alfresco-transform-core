# Build
The `alfresco-transform-core` project uses _Travis CI_. \
The `.travis.yml` config file can be found in the root of the repository.


## Stages and Jobs
1. **Build**:  Java Build with Unit and Integration Tests, WhiteSource
2. **Release**: Publish to Quay & DockerHub, Publish the S3 staging
3. **Company Release**: Publish to S3 release


## Branches
Travis CI builds differ by branch:
* `master`:
  - regular builds which include only the _Build_ stage;
  - the _Build_ stage updates the `latest` T-Engines images (only 
  from the `master` branch) on both Quay and DockerHub:
    - alfresco/alfresco-pdf-renderer
    - alfresco/alfresco-imagemagick
    - alfresco/alfresco-tika
    - alfresco/alfresco-libreoffice
  
* `ATS-*` (any branch starting with "ATS-"), `SP/1.3.N` and `SP/2.0.N`:
  - regular builds which include only the _Build_ stage;
  - although built and used on the CI agent, no docker images are updated on remote repositories;
* `release`:
  - builds that include the _Build_ and _Release_ stages;
  - PR builds with release as the target branch only execute dry runs of the actual release, 
  without actually publishing anything;
* `release/SP/1.3.N` and `release/SP/2.0.N`:
  - builds that include the _Build_ and _Release_ stages;
  - PR builds with one of the release branches as the target branch only execute dry runs of the actual release, 
without actually publishing anything;
  - the branches should be deleted once the builds are completed
* `company_release`:
  - builds that include only the `company_release` stage;
  - the `company_release` branch should be used for one-off events; once used (a build 
  completes), the branch should be deleted.

All other branches are ignored.


## Release process steps & info
Prerequisites:
 - the `master` branch has a green build and it contains all the changes that should be included in
  the next release

Steps:
1. Create a new branch from the `master` branch with the name `ATS-###_release_version`;
2. (Optional) Update the project version if the current POM version is not the next desired
 release; use a maven command, i.e. `mvn versions:set -DnewVersion=2.0.19-SNAPSHOT versions:commit`;
3. Update the project's dependencies (remove the `-SNAPSHOT` suffixes) through a new commit on the
 `ATS-###_release_version` branch;
4. Open a new Pull Request from the `ATS-###_release_version` branch into the `release` branch and
 wait for a green build; the **Release** stage on the PR build will only execute a _Dry_Run_ of
  the release;
5. Once it is approved, merge the PR through the **Create a merge commit** option (as opposed to
 _Squash and merge_ or _Rebase and merge_), delete the `ATS-###_release_version` branch, and wait 
 for a green build on the `release` branch;
6. Merge back the `release` branch into the `master` branch;
7. Update the project dependencies (append the `-SNAPSHOT` suffixes)

Steps (6) and (7) can be done either directly from an IDE or through the GitHub flow, by creating
another branch and PR. Just make sure you don't add extra commits directly to the release branch,
as this will trigger another release.

After the release, the reference deployments (docker-compose, helm) should be updated with the 
latest docker image tags.

### Release of a Service Pack (SP/1.3.N or SP/2.0.N)
Prerequisites:
 - the `SP/<version>` (<version> could be 1.3.N or 2.0.N) branch has a green build and it contains all the changes that should be included in
  the next release

**NOTE**: Make sure you release the proper version and the `SP/<version>` is merged into the correct `release/SP/<version>` (both having the same version).
E.g. When releasing a 1.3.N Service Pack, a new branch (`ATS-###_release_version`) should be created from `SP/1.3.N` and the PR should target the `release/SP/1.3.N` branch.

Steps (similar to those describing the release from `master`):
1. Create a new branch from the `SP/<version>` (SP/1.3.N or SP/2.0.N) branch with the name `ATS-###_release_version`;
2. (Optional) Update the project version if the current POM version is not the next desired
 release; use a maven command, i.e. `mvn versions:set -DnewVersion=1.3.1-SNAPSHOT versions:commit`;
3. Update the project's dependencies (remove the `-SNAPSHOT` suffixes) through a new commit on the
 `ATS-###_release_version` branch;
4. Open a new Pull Request from the `ATS-###_release_version` branch into the `release/SP/<version>` branch and
 wait for a green build; the **Release** stage on the PR build will only execute a _Dry_Run_ of
  the release;
5. Once it is approved, merge the PR through the **Create a merge commit** option (as opposed to
 _Squash and merge_ or _Rebase and merge_), delete the `ATS-###_release_version` branch, and wait 
 for a green build on the `release` branch;
6. Merge back the `release/SP/<version>` branch into the `SP/<version>` branch;
7. Update the project dependencies (append the `-SNAPSHOT` suffixes)

## Company Release process steps & info
Prerequisites:
 - Engineering Release of the desired version has been done.
 
Steps:
1. Create a new `company_release` branch from `release`. This job uses the git tag to identify the
 version to be uploaded to S3 release bucket.
2. If the last commit on `company_release` branch contains `[skip_ci]` in its message it will
 prevent Travis from starting a build. Push an empty commit in order to trigger the build,
 `git commit --allow-empty -m "Company Release <version>"`. Wait for a green build on the branch.
3. Delete local and remote `company_release` branch.

### Release of a Service Pack (SP/1.3.N or SP/2.0.N)
Follow the steps described in the previous section, but instead of creating the `company_release` from `release`, it needs to be created from the proper `release/SP/<version>` (depending on what version needs the Company Release).
