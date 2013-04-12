package cdf;

public class AttributeDescriptorRecord extends Record {

    public static final int RECORD_TYPE = 4;

    private final long aedrNext_;
    private final long agrEdrHead_;
    private final int scope_;
    private final int num_;
    private final int ngrEntries_;
    private final int maxGrEntry_;
    private final int rfuA_;
    private final long azEdrHead_;
    private final int nzEntries_;
    private final int maxzEntry_;
    private final int rfuE_;
    private final String name_;

    public AttributeDescriptorRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        aedrNext_ = buf.readOffset( ptr );
        agrEdrHead_ = buf.readOffset( ptr );
        scope_ = buf.readInt( ptr );
        num_ = buf.readInt( ptr );
        ngrEntries_ = buf.readInt( ptr );
        maxGrEntry_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        azEdrHead_ = buf.readOffset( ptr );
        nzEntries_ = buf.readInt( ptr );
        maxzEntry_ = buf.readInt( ptr );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        name_ = buf.readAsciiString( ptr, 256 );
        checkEndRecord( ptr );
    }
}
