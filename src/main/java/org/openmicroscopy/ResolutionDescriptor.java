package org.openmicroscopy;

public class ResolutionDescriptor {
    /** Resolution index (0 = the original image). */
    Integer resolutionNumber;

    /** Image width at this resolution. */
    Integer sizeX;

    /** Image height at this resolution. */
    Integer sizeY;

    /** Tile width at this resolution. */
    Integer tileSizeX;

    /** Tile height at this resolution. */
    Integer tileSizeY;

    /** Number of tiles along X axis. */
    Integer numberOfTilesX;

    /** Number of tiles along Y axis. */
    Integer numberOfTilesY;
}
