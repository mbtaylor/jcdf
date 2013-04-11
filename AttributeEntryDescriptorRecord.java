package cdf;

public abstract class AttributeEntryDescriptorRecord extends Record {

    private final long aedrNext_;
    private final int attrNum_;
    private final DataType dataType_;
    private final int num_;
    private final int numElems_;
    private final int rfuA_;
    private final int rfuB_;
    private final int rfuC_;
    private final int rfuD_;
    private final int rfuE_;
    private final long valueOffset_;

    private AttributeEntryDescriptorRecord( RecordPlan plan, int recordType ) {
        super( plan, recordType );
        Buf buf = plan.getBuf();
        Pointer ptr = new Pointer( plan.getContentOffset() );
        aedrNext_ = buf.readLong( ptr );
        attrNum_ = buf.readInt( ptr );
        dataType_ = getDataType( buf.readInt( ptr ) );
        num_ = buf.readInt( ptr );
        numElems_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuB_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuC_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuD_ = checkIntValue( buf.readInt( ptr ), -1 );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        valueOffset_ = ptr.get();
    }

    public abstract Object readValue();

    public static abstract class GrVariant
                                 extends AttributeEntryDescriptorRecord {
        public static final int RECORD_TYPE = 5;
        public GrVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE );
        }
    }

    public static abstract class ZVariant
                                 extends AttributeEntryDescriptorRecord {
        public static final int RECORD_TYPE = 9;
        public ZVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE );
        }
    }
}
