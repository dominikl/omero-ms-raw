package org.openmicroscopy;

import loci.common.Region;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BioformatsToRaw {

    static final String BF_DIR = "/home/dominik/bioformats2raw/build/distributions/bioformats2raw-0.2.0-SNAPSHOT";
    static final String REPO_DIR = "/home/dominik/test_sv/data/ManagedRepository";
    static final String N5_DIR = "/home/dominik/tmp/nd5";

    public static byte[] run(String path, String name) {
        try {
            File rawDir = new File(N5_DIR+"/"+path+"/"+name);
            if (!rawDir.exists()) {
                rawDir.mkdir();
                ProcessBuilder pb = new ProcessBuilder("bin/bioformats2raw", REPO_DIR + "/" + path + "/" + name, rawDir.getAbsolutePath());
                pb.directory(new File(BF_DIR));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
            }
            N5Provider prov = new N5Provider(rawDir.getAbsolutePath());
            return prov.readBytes(0, 0, 0, 0, 0, 0 );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
