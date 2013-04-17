package cdf;

public abstract class AttributeEntryDescriptorRecord extends Record {

    public final long aedrNext_;
    public final int attrNum_;
    public final int dataType_;
    public final int num_;
    public final int numElems_;
    public final int rfuA_;
    public final int rfuB_;
    public final int rfuC_;
    public final int rfuD_;
    public final int rfuE_;
    private final long valueOffset_;

    private AttributeEntryDescriptorRecord( RecordPlan plan, int recordType ) {
        super( plan, recordType );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        aedrNext_ = buf.readOffset( ptr );
        attrNum_ = buf.readInt( ptr );
        dataType_ = buf.readInt( ptr );
        num_ = buf.readInt( ptr );
        numElems_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuB_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuC_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuD_ = checkIntValue( buf.readInt( ptr ), -1 );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        valueOffset_ = ptr.get();
    }

    public long getValueOffset() {
        return valueOffset_;
    }

    public static class GrVariant extends AttributeEntryDescriptorRecord {
        public static final int RECORD_TYPE = 5;
        public GrVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE );
        }
    }

    public static class ZVariant extends AttributeEntryDescriptorRecord {
        public static final int RECORD_TYPE = 9;
        public ZVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE );
        }
    }
}
