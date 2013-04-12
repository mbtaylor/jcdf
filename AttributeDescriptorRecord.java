package cdf;

public class AttributeDescriptorRecord extends Record {

    public static final int RECORD_TYPE = 4;

    public final long aedrNext_;
    public final long agrEdrHead_;
    public final int scope_;
    public final int num_;
    public final int ngrEntries_;
    public final int maxGrEntry_;
    public final int rfuA_;
    public final long azEdrHead_;
    public final int nzEntries_;
    public final int maxzEntry_;
    public final int rfuE_;
    public final String name_;

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
