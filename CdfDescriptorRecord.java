package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type CDF Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CdfDescriptorRecord extends Record {

    public final long gdrOffset_;
    public final int version_;
    public final int release_;
    public final int encoding_;
    public final int flags_;
    public final int rfuA_;
    public final int rfuB_;
    public final int increment_;
    public final int rfuD_;
    public final int rfuE_;
    public final String[] copyright_;

    /**
     * Constructor.
     *
     * @param   plan   basic record information
     */
    public CdfDescriptorRecord( RecordPlan plan ) throws IOException {
        super( plan, "CDR", 1 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        gdrOffset_ = buf.readOffset( ptr );
        version_ = buf.readInt( ptr );
        release_ = buf.readInt( ptr );
        encoding_ = buf.readInt( ptr );
        flags_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuB_ = checkIntValue( buf.readInt( ptr ), 0 );
        increment_ = buf.readInt( ptr );
        rfuD_ = checkIntValue( buf.readInt( ptr ), -1 );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        int crLeng = versionAtLeast( 2, 5 ) ? 256 : 1945;
        copyright_ = toLines( buf.readAsciiString( ptr, crLeng ) );
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
        return version_ > targetVersion
            || version_ == targetVersion && release_ >= targetRelease;
    }
}
