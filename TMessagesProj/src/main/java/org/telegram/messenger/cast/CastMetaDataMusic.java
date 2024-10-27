package org.telegram.messenger.cast;

public class CastMetaDataMusic extends CastMetadata {

    private final String title;
    private final String author;

    public CastMetaDataMusic(String filename, String mimeType, long size, String title, String author) {
        super(filename, mimeType, size);
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
