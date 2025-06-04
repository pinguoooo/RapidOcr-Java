package io.github.mymonstercat;

import io.github.mymonstercat.loader.LibraryLoader;

import java.io.IOException;

/**
 * @author Monster
 */
public class NcnnMacX8664LibraryLoader implements LibraryLoader {

    @Override
    public void loadLibrary() throws IOException {
        JarFileUtil.copyFileFromJar("lib/libRapidOcr-x86_64.dylib", "/ncnn", true, false);
    }
}
