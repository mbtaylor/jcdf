package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type CDF Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CdfDescriptorRecord extends Record {

    public final long gdrOffset;
    public final int version;
    public final int release;
    public final int encoding;
    public final int flags;
    public final int rfuA;
    public final int rfuB;
    public final int increment;
    public final int rfuD;
    public final int rfuE;
    public final String[] copyright;

    /**
     * Constructor.
     *
     * @param   plan   basic record information
     */
    public CdfDescriptorRecord( RecordPlan plan ) throws IOException {
        super( plan, "CDR", 1 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.gdrOffset = buf.readOffset( ptr );
        this.version = buf.readInt( ptr );
        this.release = buf.readInt( ptr );
        this.encoding = buf.readInt( ptr );
        this.flags = buf.readInt( ptr );
        this.rfuA = checkIntValue( buf.readInt( ptr ), 0 );
        this.rfuB = checkIntValue( buf.readInt( ptr ), 0 );
        this.increment = buf.readInt( ptr );
        this.rfuD = checkIntValue( buf.readInt( ptr ), -1 );
        this.rfuE = checkIntValue( buf.readInt( ptr ), -1 );
        int crLeng = versionAtLeast( 2, 5 ) ? 256 : 1945;
        this.copyright = toLines( buf.readAsciiString( ptr, crLeng ) );
        checkEndRecord( ptr );
    }

    /**
     * Determines whether this CDR represents a CDF version of equal to
     * or greater than a given target version.
     *
     * @param   targetVersion  major version number to test against
     * @param   targetRelease  minor version number to test against
     * @return  true iff this version is at least targetVersion.targetRelease
     */
    private boolean versionAtLeast( int targetVersion, int targetRelease ) {
        return this.version > targetVersion
            || this.version == targetVersion && this.release >= targetRelease;
    }
}
