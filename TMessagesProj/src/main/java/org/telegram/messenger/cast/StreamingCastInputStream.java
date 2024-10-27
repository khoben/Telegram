package org.telegram.messenger.cast;

import org.telegram.messenger.FileStreamLoadOperation;

import java.io.IOException;
import java.io.InputStream;

public class StreamingCastInputStream extends InputStream {

    private final FileStreamLoadOperation op;

    public StreamingCastInputStream(FileStreamLoadOperation op) {
        this.op = op;
    }

    @Override
    public int read() throws IOException {
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return op.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        op.close();
    }
}
