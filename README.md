GEO Ingestor
============

Custom utility for converting the metadata in [GEOmedatdb](http://gbnci.abcc.ncifcrf.gov/geo/) 
to instances of the CEDAR Investigation Model.

#### Building Prerequisites

To build this library you must have the following items installed:

+ [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
+ A tool for checking out a [Git](http://git-scm.com/) repository.
+ Apache's [Maven](http://maven.apache.org/index.html).

#### Building

Get a copy of the latest code:

    git clone https://github.com/metadatacenter/geo-ingestor.git 

Change into the geo-ingestor directory:

    cd geo-ingestor

Then build it with Maven:

    mvn clean install

On build completion your local Maven repository will contain the generated geo-ingestor-${version}.jar and a fat JAR called geo-ingestor-${version}-jar-with-dependencies.jar.
The ./target directory will also contain these JARs.

#### Running

To run using the fat JAR:

```
   java -jar ./target/geo-ingestor-${version}-jar-with-dependencies.jar <GEOmetadb Database File> <CEDAR JSON Instances Directory> <Start Series Index> <Number of Series>
```

To run ```GEOmetadb2GEOFlat``` with Maven:

```
   mvn exec:java -Dexec.args="<GEOmetadb Database File> <CEDAR JSON Instances Directory> <Start Series Index> <Number of Series>"
```


