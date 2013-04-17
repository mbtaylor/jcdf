package cdf;

public class CompressedVariableValuesRecord extends Record {

    public static final int RECORD_TYPE = 13;

    public final int rfuA_;
    public final long cSize_;
    private final long dataOffset_;

    public CompressedVariableValuesRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        cSize_ = buf.readOffset( ptr );
        dataOffset_ = ptr.get();
    }

    public long getDataOffset() {
        return dataOffset_;
    }
}
