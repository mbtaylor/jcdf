package cdf;

import java.io.IOException;

public class CompressedCdfRecord extends Record {

    public final long cprOffset_;
    public final long uSize_;
    public final int rfuA_;
    private final long dataOffset_;

    public CompressedCdfRecord( RecordPlan plan ) throws IOException {
        super( plan, "CCR", 10 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        cprOffset_ = buf.readOffset( ptr );
        uSize_ = buf.readOffset( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        dataOffset_ = ptr.get();
    }

    public long getDataOffset() {
        return dataOffset_;
    }
}
