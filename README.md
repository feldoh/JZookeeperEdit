JZookeeperEdit
==============

A simple tool for browsing and modifying zookeeper trees.


Config
=======
On first run it will create a config file in your home directory .JZookeeperEdit
where it will save details of servers you connect to if you click "Save Cluster Details"


Requirements
==============
This tool was written using Java 1.8 with JavaFX and Maven as its build tool.
This means you will need the environment variable JAVA_HOME to point to a valid Java 8 Home.


Running using Maven
====================
mvn jfx:run


Creating a Jar
===============
mvn jfx:jar


Libraries Used
===============
Maven           - Lifecycle
Java 1.8        - Language
JavaFX 8        - GUI
ControlsFX      - Dialogues
Apache Curator  - Simple ZooKeeper Wrapper
