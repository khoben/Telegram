package org.telegram.messenger.cast;

import org.telegram.messenger.FileStreamLoadOperation;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class StreamingCastPath implements CastPath {

    private final FileStreamLoadOperation op;

    public StreamingCastPath(FileStreamLoadOperation op) {
        this.op = op;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new StreamingCastInputStream(op);
    }

    @Override
    public long getSize() {
        return -1L;
    }
}