package com.falniak.devdoctor;

import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Version provider that reads the version from the JAR manifest.
 * The version is set by Maven during the build process.
 */
public class VersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
        String version = getVersionFromManifest();
        if (version != null && !version.isEmpty()) {
            return new String[]{"devdoctor " + version};
        }
        return new String[]{"devdoctor (version unknown)"};
    }

    private String getVersionFromManifest() {
        try {
            InputStream manifestStream = getClass().getClassLoader()
                    .getResourceAsStream("META-INF/MANIFEST.MF");
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                String version = manifest.getMainAttributes()
                        .getValue("Implementation-Version");
                manifestStream.close();
                return version;
            }
        } catch (IOException e) {
            // Fall through to return null
        }
        return null;
    }
}
