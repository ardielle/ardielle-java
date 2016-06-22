# ardielle-java
Java language support for RDL

## Regenerating models from RDL

The file [rdl.rdl](https://github.com/ardielle/ardielle-common/blob/master/rdl.rdl) must be available to regenerate the sources. Assuming the clone directory for ardielle-common is specified by the environment variable `ARDIELLE_COMMON`:

    rdl -sp generate -te --ns com.yahoo.rdl java-model $ARDIELLE_COMMON/rdl.rdl


## License

Copyright 2015 Yahoo Inc.

Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.

