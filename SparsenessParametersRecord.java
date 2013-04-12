package cdf;

public class SparsenessParametersRecord extends Record {

    public static final int RECORD_TYPE = 12;

    public final int sArraysType_;
    public final int rfuA_;
    public final int pCount_;
    public final int[] sArraysParms_;

    public SparsenessParametersRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        sArraysType_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        pCount_ = buf.readInt( ptr );
        sArraysParms_ = readIntArray( buf, ptr, pCount_ );
        checkEndRecord( ptr );
    }
}
