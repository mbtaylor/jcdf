package cdf;

public class GlobalDescriptorRecord extends Record {

    public final long rVdrHead_;
    public final long zVdrHead_;
    public final long adrHead_;
    public final long eof_;
    public final int nrVars_;
    public final int numAttr_;
    public final int rMaxRec_;
    public final int rNumDims_;
    public final int nzVars_;
    public final long uirHead_;
    public final int rfuC_;
    public final int rfuD_;
    public final int rfuE_;
    public final int[] rDimSizes_;

    public GlobalDescriptorRecord( RecordPlan plan ) {
        super( plan, 2 );
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
