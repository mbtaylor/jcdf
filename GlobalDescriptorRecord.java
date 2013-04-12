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

    public GlobalDescriptorRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        rVdrHead_ = buf.readOffset( ptr );
        zVdrHead_ = buf.readOffset( ptr );
        adrHead_ = buf.readOffset( ptr );
        eof_ = buf.readOffset( ptr );
        nrVars_ = buf.readInt( ptr );
        numAttr_ = buf.readInt( ptr );
        rMaxRec_ = buf.readInt( ptr );
        rNumDims_ = buf.readInt( ptr );
        nzVars_ = buf.readInt( ptr );
        uirHead_ = buf.readOffset( ptr );
        rfuC_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuD_ = checkIntValue( buf.readInt( ptr ), -1 );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        rDimSizes_ = readIntArray( buf , ptr, rNumDims_ );
        checkEndRecord( ptr );
    }
}
