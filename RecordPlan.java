package cdf;

public class RecordPlan {

    private final long start_;
    private final long recSize_;
    private final int recType_;
    private final Buf buf_;
    
    public RecordPlan( long start, long recSize, int recType, Buf buf ) {
        start_ = start;
        recSize_ = recSize;
        recType_ = recType;
        buf_ = buf;
    }

    public long getRecordSize() {
        return recSize_;
    }

    public int getRecordType() {
        return recType_;
    }

    public Buf getBuf() {
        return buf_;
    }

    public Pointer createContentPointer() {
        return new Pointer( start_ + 12 );
    }

    public long getReadCount( Pointer ptr ) {
        return ptr.get() - start_;
    }
}
