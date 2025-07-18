### Contributing
Thanks for your interest in contributing to this project!

The following is a set of guidelines for contributing to this library. Most of them will make the life of the reviewer easier and therefore decrease the time required for the patch be included in the next version.

The project uses [pre-commit](https://pre-commit.com/) to format code (with [Spotless](https://github.com/diffplug/spotless)), validate license headers and check for secrets (with [detect-secrets](https://github.com/Yelp/detect-secrets)). To install the pre-commit hooks then first install pre-commit and then run:
```shell
pre-commit install
```
When you make a commit then these hooks will run and check the modified files. If it makes changes then you can review them and then `git commit` again to accept the changes.

#### Code Quality
This project uses `spotless` that enforces `alfresco-formatter.xml` to ensure code quality.
The code style definition file is taken always form the `master` branch of `alfresco-community-repo`.
All downstream projects use this code style definition file as well.

To check code-style violations you can use:
```bash
mvn spotless:check
```
To reformat files you can use:
```bash
mvn spotless:apply
```

#### Secret Detection

We are using [detect-secrets](https://github.com/Yelp/detect-secrets) to try to avoid accidentally publishing secret keys.
If you have pre-commit installed then this should run automatically when making a commit. Usually there should be no issues,
but if it finds a potential issue (e.g. a high entropy string) then you will see the following:

```shell
Detect secrets...........................................................Failed
- hook id: detect-secrets
- exit code: 1

ERROR: Potential secrets about to be committed to git repo!

Secret Type: Secret Keyword
Location:    test.txt:1
```

If this is a false positive and you actually want to commit the string then run these two commands:

```shell
detect-secrets scan --baseline .secrets.baseline
detect-secrets audit .secrets.baseline
```

This will update the baseline file to include your new code and then allow you to review the detected secret and mark it as a false positive.
Once you are finished then you can add `.secrets.baseline` to the staged changes and you should be able to create a commit.
