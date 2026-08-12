### Contributing

Thanks for your interest in contributing to this project!

The following is a set of guidelines for contributing to this library. Most of them will make the life of the reviewer easier and therefore decrease the time required for the patch be included in the next version.

The project uses [pre-commit](https://pre-commit.com/) to format code (with [Spotless](https://github.com/diffplug/spotless)) and validate license headers. To install the pre-commit hooks then first install pre-commit and then run:

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
