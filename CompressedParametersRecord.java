package cdf;

public class CompressedParametersRecord extends Record {

    public static final int RECORD_TYPE = 11;

    public final int cType_;
    public final int rfuA_;
    public final int pCount_;
    public final int[] cParms_;

    public CompressedParametersRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        cType_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        pCount_ = buf.readInt( ptr );
        cParms_ = readIntArray( buf, ptr, pCount_ );
        checkEndRecord( ptr );
    }
}
