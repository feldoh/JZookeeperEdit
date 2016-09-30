[![Build Status](https://travis-ci.org/feldoh/JZookeeperEdit.svg?branch=master)](https://travis-ci.org/feldoh/JZookeeperEdit)
[![Download](https://api.bintray.com/packages/feldoh/JZookeeperEdit/JZookeeperEdit/images/download.svg) ](https://bintray.com/feldoh/JZookeeperEdit/JZookeeperEdit/_latestVersion)
[![Stories in Ready](https://badge.waffle.io/feldoh/JZookeeperEdit.png?label=ready&title=Ready)](https://waffle.io/feldoh/JZookeeperEdit)
JZookeeperEdit
==============

A simple tool for browsing and modifying zookeeper trees.


Config
=======
On first run it will create a config file in your home directory .JZookeeperEdit
where it will save details of servers you connect to if you click "Save Cluster Details"


Get a pre-built Binary
======================
Pre-built binaries are available for most common platforms on [Bintray](https://bintray.com/feldoh/JZookeeperEdit/JZookeeperEdit)


Requirements
==============
This tool was written using Java 1.8 with JavaFX and Maven as its build tool.
This means you will need the environment variable JAVA_HOME to point to a valid Java 8 Home.

**Note that due to a limitation of controlsfx you will need to use a version of this tool appropriate for your version of Java. This version of the tool requires at least Java 1.8.0_40**<br>
The reason for this is discussed in [this](http://fxexperience.com/2014/09/announcing-controlsfx-8-20-7/) blog post

Running using Maven
====================
mvn jfx:run


Creating a Jar
===============
mvn jfx:jar


Creating a native binary
=========================
mvn jfx:native
<br>**Note that this will build a native package for whatever environment you are on**
<br>**For example, a dmg on a mac**


Libraries & Tools
=========================
JavaFX Maven Plugin 8.1.3  - Simple JavaFX + Maven<br>
Apache Curator 2.11.0      - Simple ZooKeeper Wrapper<br>
ControlsFX 8.40.12         - Dialogues<br>
Java 1.8.0_102             - Language<br>
Maven 3.3.9                - Lifecycle<br>
JavaFX 8                   - GUI

