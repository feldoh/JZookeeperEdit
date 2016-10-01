# Contrbuting To JZookeeperEdit
Thankyou for your interest in contributing to JZookeeperEdit. This project is very open to contibutions,
from people of all skill levels and backgrounds. Documentation, Wiki changes, packaging etc are all valued.

When contributing to this repository, please first discuss the change you wish to make via issue,
you will need an issue number for your PR to be accepted.

If making CLI changes keep in mind this project aims to be CLI friendly, so thing about how calls could be chained,
ideally provide examples in the README.md.

All pull requests should target develop unless a special exemption is made such as for a critical fix.
Release versions will be regularly cut from develop after sufficient testing.

All pull requests must begin with an issue number in their commit message e.g
"#10 Add a thing"

Please note we have a code of conduct, please follow it in all your interactions with the project.

## Pull Request Process

1. If adding dependencies ensure they are appropriately scoped.
2. Ensure your code does not violate the checkstyle or findbugs configurations included.
   You can confirm this via `mvn clean verify`
3. Update the README.md with details of changes to the interface, this includes new environment 
   variables, exposed ports, useful file locations and notable parameters.
4. Increase the version numbers in any examples files and the README.md to the new version that this
   Pull Request would represent. The versioning scheme we use is [Semantic Versioning](http://semver.org/).

## Contributor Code of Conduct

As contributors and maintainers of this project, and in the interest of fostering an open and 
welcoming community, we pledge to respect all people who contribute through reporting issues, 
posting feature requests, updating documentation, submitting pull requests or patches, and other 
activities.

We are committed to making participation in this project a harassment-free experience for everyone, 
regardless of level of experience, gender, gender identity and expression, sexual orientation, 
disability, personal appearance, body size, race, ethnicity, age, religion, or nationality.

Examples of unacceptable behavior by participants include:

* The use of sexualized language or imagery
* Personal attacks
* Trolling or insulting/derogatory comments
* Public or private harassment
* Publishing other's private information, such as physical or electronic addresses, without explicit
  permission
* Other unethical or unprofessional conduct.

Project maintainers have the right and responsibility to remove, edit, or reject comments, commits, 
code, wiki edits, issues, and other contributions that are not aligned to this Code of Conduct. By 
adopting this Code of Conduct, project maintainers commit themselves to fairly and consistently 
applying these principles to every aspect of managing this project. Project maintainers who do not 
follow or enforce the Code of Conduct may be permanently removed from the project team.

This code of conduct applies both within project spaces and in public spaces when an individual is 
representing the project or its community.

Instances of abusive, harassing, or otherwise unacceptable behavior may be reported by opening an 
issue or contacting one or more of the project maintainers.

This Code of Conduct is adapted from the [Contributor Covenant](http://contributor-covenant.org), 
version 1.2.0, available at 
[http://contributor-covenant.org/version/1/2/0/](http://contributor-covenant.org/version/1/2/0/)
