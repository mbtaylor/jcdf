package cdf;

import java.io.IOException;

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

    public Pointer createContentPointer() throws IOException {
        Pointer ptr = new Pointer( start_ );

        // This is slightly wasteful (reading rather than just calculating
        // the offset), but it ensures that the content offset is correct -
        // buf may read 4 or 8 bytes for the record size.
        buf_.readOffset( ptr );  // record size
        buf_.readInt( ptr );     // record type
        return ptr;
    }

    public long getReadCount( Pointer ptr ) {
        return ptr.get() - start_;
    }
}
