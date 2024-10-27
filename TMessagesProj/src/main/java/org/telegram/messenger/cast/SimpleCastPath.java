package org.telegram.messenger.cast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class SimpleCastPath implements CastPath {
    
    private final File file;
    
    public SimpleCastPath(String path) {
        this.file = new File(path);
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(this.file);
    }

    @Override
    public long getSize() {
       return this.file.length();
    }
}
