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

    public Buf getBuf() {
        return buf_;
    }

    public long getContentOffset() {
        return start_ + 12;
    }

    public int getRecordType() {
        return recType_;
    }
}
