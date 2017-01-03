# ardielle-java
Java language support for RDL

## Regenerating models from RDL

The file [rdl.rdl](https://github.com/ardielle/ardielle-common/blob/master/rdl.rdl) must be available to regenerate the sources. Assuming the clone directory for ardielle-common is specified by the environment variable `ARDIELLE_COMMON`:

    rdl -sp generate -te --ns com.yahoo.rdl java-model $ARDIELLE_COMMON/rdl.rdl

## Maven

``` xml
<project>
  <dependencies>
    <dependency>
      <groupId>com.yahoo.rdl</groupId>
      <artifactId>rdl-java</artifactId>
      <version>1.4.9</version>
    </dependency>
  </dependencies>
  <repositories>
    <repository>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
      <id>bintray-yahoo-maven</id>
      <name>bintray-plugins</name>
      <url>http://yahoo.bintray.com/maven</url>
    </repository>
  </repositories>
</project>
```

``` sh
mvn versions:use-latest-releases "-Dincludes=com.yahoo.rdl"
```

## License

Copyright 2015-2017 Yahoo Inc.

Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.

