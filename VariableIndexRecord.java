package cdf;

public class VariableIndexRecord extends Record {

    public static final int RECORD_TYPE = 6;

    public final long vxrNext_;
    public final int nEntries_;
    public final int nUsedEntries_;
    public final int[] first_;
    public final int[] last_;
    public final long[] offset_;

    public VariableIndexRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        vxrNext_ = buf.readOffset( ptr );
        nEntries_ = buf.readInt( ptr );
        nUsedEntries_ = buf.readInt( ptr );
        first_ = readIntArray( buf, ptr, nEntries_ );
        last_ = readIntArray( buf, ptr, nEntries_ );
        offset_ = readOffsetArray( buf, ptr, nEntries_ );
        checkEndRecord( ptr );
    }
}
