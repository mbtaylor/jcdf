package cdf;

public class GlobalDescriptorRecord extends Record {

    public static final int RECORD_TYPE = 2;

    private final long rVdrHead_;
    private final long zVdrHead_;
    private final long adrHead_;
    private final long eof_;
    private final int nrVars_;
    private final int numAttr_;
    private final int rMaxRec_;
    private final int rNumDims_;
    private final int nzVars_;
    private final long uirHead_;
    private final int rfuC_;
    private final int rfuD_;
    private final int rfuE_;
    private final int[] rDimSizes_;

    public GlobalDescriptorRecord( long recSize, int recType,
                                   Buf buf, Offset offset12 ) {
        super( recSize, recType, RECORD_TYPE );
        Offset off = offset12;

        rVdrHead_ = buf.readLong( off );
        zVdrHead_ = buf.readLong( off );
        adrHead_ = buf.readLong( off );
        eof_ = buf.readLong( off );
        nrVars_ = buf.readInt( off );
        numAttr_ = buf.readInt( off );
        rMaxRec_ = buf.readInt( off );
        rNumDims_ = buf.readInt( off );
        nzVars_ = buf.readInt( off );
        uirHead_ = buf.readLong( off );
        rfuC_ = checkIntValue( buf.readInt( off ), 0 );
        rfuD_ = checkIntValue( buf.readInt( off ), -1 );
        rfuE_ = checkIntValue( buf.readInt( off ), -1 );
        rDimSizes_ = readIntArray( buf , off, rNumDims_ );
    }
}
