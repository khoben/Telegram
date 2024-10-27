package org.telegram.messenger.cast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface CastPath {
    InputStream getInputStream() throws FileNotFoundException;
    long getSize();
}

