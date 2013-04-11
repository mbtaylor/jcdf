package cdf;

public class CompressedCdfRecord extends Record {

    public static final int RECORD_TYPE = 10;

    private final long cprOffset_;
    private final long uSize_;
    private final int rfuA_;
    private final long dataOffset_;

    public CompressedCdfRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        cprOffset_ = buf.readLong( ptr );
        uSize_ = buf.readLong( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        dataOffset_ = ptr.get();
    }
}
