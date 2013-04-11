package cdf;

public class CompressedParametersRecord extends Record {

    public static final int RECORD_TYPE = 11;

    private final int cType_;
    private final int rfuA_;
    private final int pCount_;
    private final int[] cParms_;

    public CompressedParametersRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = new Pointer( plan.getContentOffset() );
        cType_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        pCount_ = buf.readInt( ptr );
        cParms_ = readIntArray( buf, ptr, pCount_ );
        checkEndRecord( ptr );
    }
}
