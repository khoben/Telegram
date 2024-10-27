package org.telegram.messenger.cast;

public class CastMetadata {
    private final String filename;
    private final String mimeType;

    private final long size;

    public CastMetadata(String filename, String mimeType, long size) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }
}
