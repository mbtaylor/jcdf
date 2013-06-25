package cdf.record;

import java.io.IOException;

/**
 * Abstract superclass for CDF Attribute Entry Descriptor Records.
 * Two concrete subclasses exist for AzEDRs and AgrEDRs.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
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

    /**
     * Constructor.
     *
     * @param  plan  basic record info
     * @param  abbrev  abbreviated name for record type
     * @param  recordType  record type code
     */
    private AttributeEntryDescriptorRecord( RecordPlan plan, String abbrev,
                                            int recordType )
            throws IOException {
        super( plan, abbrev, recordType );
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

    /**
     * Returns the file offset at which this record's Value field starts.
     *
     * @return  file offset of Value field
     */
    public long getValueOffset() {
        return valueOffset_;
    }

    /**
     * Field data for CDF record of type Attribute g/rEntry Descriptor Record.
     */
    public static class GrVariant extends AttributeEntryDescriptorRecord {

        /**
         * Constructor.
         *
         * @param  plan   basic record information
         */
        public GrVariant( RecordPlan plan ) throws IOException {
            super( plan, "AgrEDR", 5 );
        }
    }

    /**
     * Field data for CDF record of type Attribute zEntry Descriptor Record.
     */
    public static class ZVariant extends AttributeEntryDescriptorRecord {

        /**
         * Constructor.
         *
         * @param  plan   basic record information
         */
        public ZVariant( RecordPlan plan ) throws IOException {
            super( plan, "AzEDR", 9 );
        }
    }
}
