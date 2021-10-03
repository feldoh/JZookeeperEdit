![JZookeeperEdit Icon](logo.png)

JZookeeperEdit
==============

[![Build Status](https://app.travis-ci.com/feldoh/JZookeeperEdit.svg?branch=develop)](https://app.travis-ci.com/github/feldoh/JZookeeperEdit)
[![Join the chat at https://gitter.im/JZookeeperEdit/Lobby](https://badges.gitter.im/JZookeeperEdit/Lobby.svg)](https://gitter.im/JZookeeperEdit/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Coverage Status](https://coveralls.io/repos/github/feldoh/JZookeeperEdit/badge.svg)](https://coveralls.io/github/feldoh/JZookeeperEdit)

A simple tool for browsing and modifying zookeeper trees.


Config
=======
On first run it will create a config file in your home directory .JZookeeperEdit
where it will save details of servers you connect to if you click "Save Cluster Details"


Requirements
==============
This tool was written using Java 11 with OpenFX and Maven as its build tool.
This means you will need the environment variable JAVA_HOME to point to a valid Java 11 Home.


Using the CLI
=============
Zookeeper comes with a nice little command line tool for exploration. However for scripting the metadata and the format can be a pain.
This tool comes with a limited (but more customisable in terms of output) cli of its own. The main aim here was to make a zkCli that was a little more unix-tools friendly.

You can see the options by running `java -jar JZookeeperEdit.jar -h`.

Several notable features include:

* The ability to chain ls calls. For example to get all children of the children of the root one could run
    `java -jar JZookeeperEdit.jar -l -p -c localhost / | xargs java -jar JZookeeperEdit.jar -l -c localhost`
    Naturally you can extend this via some grepping or awk commands or even asking questions like "do the children in one cluster have children in another".
    Note that if you wish to chain calls you will need to use the `-p` flag to have it print the full paths.
* The ability to extract particular elements from metadata and a more unix tools friendly meta output i.e.:
    `java -jar JZookeeperEdit.jar -m -f getNumChildren -c localhost /`
    Note that a full list of available accessors is available via `java -jar JZookeeperEdit.jar -a`


Running using Maven
====================
mvn javafx:run


CodeSee
========
You can see an overview of the structure of this project with a tour using this [CodeSee Map](https://app.codesee.io/maps/public/bd79ede0-20a5-11ec-8e87-072584a095ef)

There is also a second simplified map for if you just want to understand the [CLI](https://app.codesee.io/maps/public/63599120-2484-11ec-95de-15c6708e23a1)


Libraries & Tools
=========================
OpenFX Maven Plugin 0.0.3  - Simple OpenFX + Maven<br>
Apache Curator 2.11.0      - Simple ZooKeeper Wrapper<br>
ControlsFX 11.0.0          - Dialogues<br>
Java 11                    - Language<br>
Maven 3.3.9                - Lifecycle<br>
OpenFX 11                  - GUI
