/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.tbin;
import java.io.IOException;

/**
 * TBin exception
 */
public class TBinException extends IOException {
    TBinException(String msg) {
        super(msg);
    }
}
