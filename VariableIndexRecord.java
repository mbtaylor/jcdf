package cdf;

public class VariableIndexRecord extends Record {

    public static final int RECORD_TYPE = 6;

    private final long vxrNext_;
    private final int nEntries_;
    private final int nUsedEntries_;
    private final int[] first_;
    private final int[] last_;
    private final long[] offset_;

    public VariableIndexRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = new Pointer( plan.getContentOffset() );
        vxrNext_ = buf.readInt( ptr );
        nEntries_ = buf.readInt( ptr );
        nUsedEntries_ = buf.readInt( ptr );
        first_ = readIntArray( buf, ptr, nEntries_ );
        last_ = readIntArray( buf, ptr, nEntries_ );
        offset_ = readLongArray( buf, ptr, nEntries_ );
        checkEndRecord( ptr );
    }
}
