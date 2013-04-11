package cdf;

public class UnusedInternalRecord extends Record {

    public static final int RECORD_TYPE = -1;

    private final long nextUirOffset_;
    private final long prevUirOffset_;

    public UnusedInternalRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );

        // In case this is an Unsociable UIR, don't actually read the
        // next/prev link offsets here, since they might not exist,
        // and might even if very unlucky cause an error by attempting
        // to read off the end of the file.  Just record their positions.
        Pointer ptr = new Pointer( plan.getContentOffset() );
        nextUirOffset_ = ptr.getAndIncrement( 8 );
        prevUirOffset_ = ptr.getAndIncrement( 8 );
    }
}
