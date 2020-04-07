package org.openmicroscopy;

import loci.common.DataTools;
import loci.common.Region;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ome.OMEPyramidStore;
import loci.formats.services.OMEXMLService;
import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.DoubleArrayDataBlock;
import org.janelia.saalfeldlab.n5.FloatArrayDataBlock;
import org.janelia.saalfeldlab.n5.IntArrayDataBlock;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.ShortArrayDataBlock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class N5Provider {

    private static final int PYRAMID_SCALE = 2;

    private N5FSReader reader;
    private String metadataFile;

    private boolean interleaved, littleEndian;
    private int pixelType, numberOfResolutions, rgbChannels, z, c, t, planeCount;
    private List<ResolutionDescriptor> resolutions;
    private OMEPyramidStore metadata;

    public N5Provider(String n5Path) throws Exception {
        this.reader = new N5FSReader(n5Path+"/pyramid.n5");
        this.metadataFile = n5Path+"/METADATA.ome.xml";
        populateMetadataFromInputFile();
    }

    public byte[] readBytes(int resolution,
                                    int no, int x, int y, int w, int h)
            throws IOException
    {
        ResolutionDescriptor descriptor = resolutions.get(resolution);
        int bpp = FormatTools.getBytesPerPixel(pixelType);
        int xy = descriptor.tileSizeX * descriptor.tileSizeY;
        if (w > 0 && h > 0) {
            xy = w * h;
        }

        String blockPath = "/" + resolution;
        long[] gridPosition = new long[] {x, y, no};
        DataBlock block = reader.readBlock(
                blockPath, reader.getDatasetAttributes(blockPath),
                gridPosition);
        ByteBuffer buffer = block.toByteBuffer();
        byte[] tile = new byte[xy * bpp * rgbChannels];
        boolean isPadded = buffer.limit() > tile.length;
        if (w < 1 || h < 1 || (w == descriptor.tileSizeX &&
                h == descriptor.tileSizeY))
        {
            buffer.get(tile);
        }
        else {
            for (int ch=0; ch<rgbChannels; ch++) {
                int tilePos = ch * xy * bpp;
                int pos = ch * descriptor.tileSizeX * descriptor.tileSizeY * bpp;
                buffer.position(pos);
                for (int row=0; row<h; row++) {
                    buffer.get(tile, tilePos, w * bpp);
                    if (isPadded) {
                        buffer.position(buffer.position() +
                                (descriptor.tileSizeX - w) * bpp);
                    }
                    tilePos += w * bpp;
                }
            }
        }
        return tile;
    }

    private void populateMetadataFromInputFile() throws Exception
    {
        numberOfResolutions = reader.list("/").length;
        System.out.println("Resolutions: "+numberOfResolutions);
        interleaved = false;
        rgbChannels = 1;
        String blockPath = "/0";
        DatasetAttributes datt = reader.getDatasetAttributes(blockPath);
        System.out.println("DatasetAttributes: "+datt);
        DataBlock block = reader.readBlock(blockPath, datt, new long[] {0, 0, 0});
        littleEndian = block.toByteBuffer().order() == ByteOrder.LITTLE_ENDIAN;
        if (block instanceof ByteArrayDataBlock) {
            pixelType = FormatTools.UINT8;
        }
        else if (block instanceof ShortArrayDataBlock) {
            pixelType = FormatTools.UINT16;
        }
        else if (block instanceof IntArrayDataBlock) {
            pixelType = FormatTools.UINT32;
        }
        else if (block instanceof FloatArrayDataBlock) {
            pixelType = FormatTools.FLOAT;
        }
        else if (block instanceof DoubleArrayDataBlock) {
            pixelType = FormatTools.DOUBLE;
        }
        else {
            throw new FormatException("Unsupported block type: " + block);
        }

        OMEXMLService service = new ServiceFactory().getInstance(OMEXMLService.class);
        if (service != null) {
            String xml = DataTools.readFile(metadataFile);
            try {
                if (xml != null) {
                    metadata = (OMEPyramidStore) service.createOMEXMLMetadata(xml);

                    z = metadata.getPixelsSizeZ(0).getNumberValue().intValue();
                    c = metadata.getPixelsSizeC(0).getNumberValue().intValue();
                    t = metadata.getPixelsSizeT(0).getNumberValue().intValue();
                    rgbChannels = metadata.getChannelSamplesPerPixel(
                            0, 0).getNumberValue().intValue();
                    planeCount = (z * c * t) / rgbChannels;
                    c /= rgbChannels;
                    littleEndian = !metadata.getPixelsBigEndian(0);
                }
                else {
                    metadata = (OMEPyramidStore) service.createOMEXMLMetadata();
                }
            }
            catch (ServiceException e) {
                throw new FormatException("Could not parse OME-XML", e);
            }
        }

        describePyramid();
    }

    /**
     * Calculate image width and height for each resolution.
     * Uses the first tile in the resolution to find the tile size.
     */
    private void describePyramid() throws FormatException, IOException {
        resolutions = new ArrayList<ResolutionDescriptor>();
        for (int resolution = 0; resolution < numberOfResolutions; resolution++) {
            ResolutionDescriptor descriptor = new ResolutionDescriptor();
            descriptor.resolutionNumber = resolution;

            DatasetAttributes attrs = reader.getDatasetAttributes("/" + resolution);
            descriptor.tileSizeX = attrs.getBlockSize()[0];
            descriptor.tileSizeY = attrs.getBlockSize()[1];
            rgbChannels = attrs.getBlockSize()[2];
            descriptor.numberOfTilesX =
                    (int) Math.ceil(
                            (double) attrs.getDimensions()[0] / descriptor.tileSizeX);
            descriptor.numberOfTilesY =
                    (int) Math.ceil(
                            (double) attrs.getDimensions()[1] / descriptor.tileSizeY);

            if (resolution == 0) {
                if (metadata.getImageCount() > 0) {
                    descriptor.sizeX =
                            metadata.getPixelsSizeX(0).getNumberValue().intValue();
                    descriptor.sizeY =
                            metadata.getPixelsSizeY(0).getNumberValue().intValue();
                }
                else {
                    descriptor.sizeX = descriptor.tileSizeX * descriptor.numberOfTilesX;
                    descriptor.sizeY = descriptor.tileSizeY * descriptor.numberOfTilesY;
                }
            }
            else {
                descriptor.sizeX =
                        resolutions.get(resolution - 1).sizeX / PYRAMID_SCALE;
                descriptor.sizeY =
                        resolutions.get(resolution - 1).sizeY / PYRAMID_SCALE;
            }

            resolutions.add(descriptor);
        }
    }
}
