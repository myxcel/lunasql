<img src="lunasql-figlet.png" alt="LunaSQL Logo" />

*LunaSQL* - a fair SQL shell client
===================================

<!-- TABLE OF CONTENTS -->  
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#features-and-motivation">Features and motivation</a></li>
    <li><a href="#getting-started">Getting started</a></li>
    <li><a href="#how-to-contribute">How to contribute</a></li>
    <li><a href="#external-libraries">External libraries</a></li>
    <li><a href="#checking-integrity">Checking integrity</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details><br />

[![Java CI](https://github.com/auditum-mpa/lunasql/actions/workflows/ant.yml/badge.svg)](https://github.com/auditum-mpa/lunasql/actions/workflows/ant.yml) -
[![Discuss](https://img.shields.io/badge/version-4.9.1.3-green)](https://github.com/auditum-mpa/lunasql/releases/tag/4.9.3.0)
[![Releases](https://img.shields.io/github/downloads/auditum-mpa/lunasql/total.svg)](https://github.com/auditum-mpa/lunasql/releases/latest) -
[![Discuss](https://img.shields.io/badge/discuss-here-magenta)](https://github.com/auditum-mpa/lunasql/discussions)
[![Wiki](https://img.shields.io/badge/consult-wiki-blue)](https://github.com/auditum-mpa/lunasql/wiki)
[![Issue](https://img.shields.io/badge/report-issue-orange)](https://github.com/auditum-mpa/lunasql/issues)

## About the project

Another naive but positive and fun Java JDBC SQL shell client. Designed to simply query databases by JDBC, or automate database tasks and prototype applications before real implementation.

This is an old personal project that I used to develop occasionally, and that I just put on GitHub under an opensource license, but the development started in 2009! So please bear with me as to the quality of the code. It works, and it can be useful!

## Features and motivation

  * connects to any database by JDBC driver,
  * sends SQL commands to database server,
  * adds predefined useful commands and macros,
  * is highly configurable by command line or file,
  * accepts user-defined macros and command plugins,
  * evaluates expressions by JSR-223,
  * embeds a minimalistic graphical IDE,
  * includes an HTTP server for remote querying,
  * secures scripts execution by digital signature,
  * provides useful javascript scripting libraries,
  * sources recently released on GitHub,
  * only one (small) jar file!

## Getting started

LunaSQL is released as a jar file, which can be run using [H2 Database driver](https://h2database.com/html/main.html) as a minimalistic command with:
```
java lunasql.Main --type=H2DB --name=path/to/base/MyDB --user=sa --password="" --console
```

To build the distribution, just run the [ant](https://ant.apache.org) task (you may have to adapt the `path id="classpath"` entry):

```
ant dist
```

## How to contribute

Any help will be greatly appreciated! You can contribute by using the application and testing it, [reporting new issues](https://github.com/auditum-mpa/lunasql/issues). You can also try to work on [good first issues](https://github.com/auditum-mpa/lunasql/contribute). Don't forget that the [Discussions page](https://github.com/auditum-mpa/lunasql/discussions) warmly welcomes you to say hello or discuss about new ideas, questions...

## External libraries

See the [src/lunasql/doc/libraries.txt](https://github.com/auditum-mpa/lunasql/blob/master/src/lunasql/doc/libraries.txt). Thank you to all libraries contributors for your good job!

## Checking integrity

First, commits are signed with PGP key fingerprint `6F9F 349C D9DB 0B1A A0EC B6DE 2EA0 CCE6 2860 3945` and are automatically verified by GitHub. Additionally, LunaSQL release library `lunasql.x.x.x.jar` file is signed by [Keybase PGP key](https://keybase.io/espritlibredev/key.asc) (fingerprint `AA77 7903 6281 D0E9 209B E8B9 2627 39EB A36C EB3E`).

To verify this release library file, simply type the following command in the download directory:

```bash
keybase pgp verify -i lunasql-x.x.x.jar -d lunasql-x.x.x.jar.asc
```

**or** if you prefer to use GnuPG:

```bash
curl https://keybase.io/espritlibredev/key.asc | gpg --import && \
  gpg --verify lunasql-x.x.x.jar.asc lunasql-x.x.x.jar
```

## License

LunaSQL is distributed under the CeCILL License. See the [french](http://cecill.info/licences/Licence_CeCILL_V2.1-fr.html) or [english](http://cecill.info/licences/Licence_CeCILL_V2.1-en.html) versions for further details.

If you like LunaSQL, please pay me a :coffee: coffee ([Stellar](https://www.stellar.org/) _espritlibredev*keybase.io_)

Enjoy!

