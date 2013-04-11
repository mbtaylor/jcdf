package cdf;

public class CdfDescriptorRecord extends Record {

    public static final int RECORD_TYPE = 1;

    private final long gdrOffset_;
    private final int version_;
    private final int release_;
    private final int encoding_;
    private final int flags_;
    private final int rfuA_;
    private final int rfuB_;
    private final int increment_;
    private final int rfuD_;
    private final int rfuE_;
    private final String[] copyright_;

    public CdfDescriptorRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = new Pointer( plan.getContentOffset() );
        gdrOffset_ = buf.readLong( ptr );
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

    public boolean versionAtLeast( int targetVersion, int targetRelease ) {
        return version_ > targetVersion
            || version_ == targetVersion && release_ >= targetRelease;
    }
}
