package cdf;

import java.io.IOException;

public class VariableIndexRecord extends Record {

    public final long vxrNext_;
    public final int nEntries_;
    public final int nUsedEntries_;
    public final int[] first_;
    public final int[] last_;
    public final long[] offset_;

    public VariableIndexRecord( RecordPlan plan ) throws IOException {
        super( plan, "VXR", 6 );
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
