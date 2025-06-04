package io.github.mymonstercat;

import io.github.mymonstercat.loader.LibraryLoader;

import java.io.IOException;

/**
 * @author Monster
 */
public class OnnxWindowsX86LibraryLoader implements LibraryLoader {

    @Override
    public void loadLibrary() throws IOException {
        JarFileUtil.copyFileFromJar("lib/RapidOcr-x86.dll", "/onnx", true, false);
    }
}
