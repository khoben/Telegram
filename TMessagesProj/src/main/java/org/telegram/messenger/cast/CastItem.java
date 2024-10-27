package org.telegram.messenger.cast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CastItem {

    private final CastPath path;
    private final String mime;
    private long size;

    public CastItem(CastPath path, String mime, long size) {
        this.path = path;
        this.mime = mime;
        this.size = size;
    }

    public String getMime() {
        return mime;
    }

    public long getSize() {
        if (size == -1L) {
            size = path.getSize();
        }
        return size;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return path.getInputStream();
    }
}
