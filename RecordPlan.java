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
        long pos = start_;
        pos += buf_.isBit64() ? 8 : 4;  // record size
        pos += 4;                       // record type
        return new Pointer( pos );
    }

    /**
     * Returns the number of bytes in this record read (or skipped) by the
     * current state of a given pointer.
     *
     * @param   ptr  pointer
     * @return  number of bytes between record start and pointer value
     */
    public long getReadCount( Pointer ptr ) {
        return ptr.get() - start_;
    }
}
