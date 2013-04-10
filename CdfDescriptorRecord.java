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

    public CdfDescriptorRecord( long recSize, int recType,
                                Buf buf, Offset offset12 ) {
        super( recSize, recType, RECORD_TYPE );
        Offset off = offset12;
        gdrOffset_ = buf.readLong( off );
        version_ = buf.readInt( off );
        release_ = buf.readInt( off );
        encoding_ = buf.readInt( off );
        flags_ = buf.readInt( off );
        rfuA_ = checkIntValue( buf.readInt( off ), 0 );
        rfuB_ = checkIntValue( buf.readInt( off ), 0 );
        increment_ = buf.readInt( off );
        rfuD_ = checkIntValue( buf.readInt( off ), -1 );
        rfuE_ = checkIntValue( buf.readInt( off ), -1 );
        int crLeng = versionAtLeast( 2, 5 ) ? 256 : 1945;
        copyright_ = toLines( buf.readAsciiString( off, crLeng ) );
    }

    public boolean versionAtLeast( int targetVersion, int targetRelease ) {
        return version_ > targetVersion
            || version_ == targetVersion && release_ >= targetRelease;
    }
}
