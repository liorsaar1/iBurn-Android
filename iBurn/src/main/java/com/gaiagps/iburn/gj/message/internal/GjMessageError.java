package com.gaiagps.iburn.gj.message.internal;

import com.gaiagps.iburn.gj.message.GjMessageString;

public class GjMessageError extends GjMessageString {

    public GjMessageError(String dataString) {
        super(Type.Error, dataString);
    }
}
