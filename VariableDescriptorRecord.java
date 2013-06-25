package cdf.record;

import cdf.DataType;
import java.io.IOException;

/**
 * Abstract superclass for CDF Variable Descriptor Records.
 * Two concrete subclasses exist for rVDRs and zVDRs.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public abstract class VariableDescriptorRecord extends Record {

    public final long vdrNext_;
    public final int dataType_;
    public final int maxRec_;
    public final long vxrHead_;
    public final long vxrTail_;
    public final int flags_;
    public final int sRecords_;
    public final int rfuB_;
    public final int rfuC_;
    public final int rfuF_;
    public final int numElems_;
    public final int num_;
    public final long cprOrSprOffset_;
    public final int blockingFactor_;
    public final String name_;
    public final int zNumDims_;
    public final int[] zDimSizes_;
    public final boolean[] dimVarys_;
    private final long padOffset_;
    private final int padBytes_;

    /**
     * Constructor.
     *
     * @param  plan  basic record info
     * @param  abbrev   abbreviated name for record type
     * @param  recordType   record type code
     * @param  hasDims  true iff the zNumDims and zDimSizes fields
     *                  will be present
     * @param  nameLeng  number of characters used for attribute names
     */
    private VariableDescriptorRecord( RecordPlan plan, String abbrev,
                                      int recordType, boolean hasDims,
                                      int nameLeng )
            throws IOException {
        super( plan, abbrev, recordType );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        vdrNext_ = buf.readOffset( ptr );
        dataType_ = buf.readInt( ptr );
        maxRec_ = buf.readInt( ptr );
        vxrHead_ = buf.readOffset( ptr );
        vxrTail_ = buf.readOffset( ptr );
        flags_ = buf.readInt( ptr );
        sRecords_ = buf.readInt( ptr );
        rfuB_ = checkIntValue( buf.readInt( ptr ), 0 );
        rfuC_ = checkIntValue( buf.readInt( ptr ), -1 );
        rfuF_ = checkIntValue( buf.readInt( ptr ), -1 );
        numElems_ = buf.readInt( ptr );
        num_ = buf.readInt( ptr );
        cprOrSprOffset_ = buf.readOffset( ptr );
        blockingFactor_ = buf.readInt( ptr );
        name_ = buf.readAsciiString( ptr, nameLeng );
        if ( hasDims ) {
            zNumDims_ = buf.readInt( ptr );
            zDimSizes_ = readIntArray( buf, ptr, zNumDims_ );
        }
        else {
            zNumDims_ = 0;
            zDimSizes_ = null;
        }
        boolean hasPad = hasBit( flags_, 1 );
        padBytes_ = hasPad ? DataType.getDataType( dataType_ ).getByteCount()
                           * numElems_
                           : 0;
        final int ndim;
        if ( hasDims ) {
            ndim = zNumDims_;
        }
        else {

            // Work out the number of dimensions of an rVariable by subtracting
            // the values of all the other fields from the record size.
            // The more direct way would be by using the rNumDims field of
            // the GDR, but we don't have access to that here.
            long runningCount = plan.getReadCount( ptr );
            long spareBytes = getRecordSize() - runningCount - padBytes_;
            assert spareBytes == (int) spareBytes;
            if ( spareBytes % 4 != 0 ) {
                warnFormat( "rVDR DimVarys field non-integer size??" );
            }
            ndim = ( (int) spareBytes ) / 4;
        }
        int[] iDimVarys = readIntArray( buf, ptr, ndim );
        dimVarys_ = new boolean[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            dimVarys_[ i ] = iDimVarys[ i ] != 0;
        }
        long padpos = ptr.getAndIncrement( padBytes_ );
        padOffset_ = hasPad ? padpos : -1L;
        checkEndRecord( ptr );
    }

    /**
     * Returns the file offset at which this record's PadValue can be found.
     * If there is no pad value, -1 is returned.
     *
     * @return  pad file offset, or -1
     */
    public long getPadValueOffset() {
        return padOffset_;
    }

    /**
     * Returns the number of bytes in the pad value.
     * If there is no pad value, 0 is returned.
     *
     * @return  pad value size in bytes
     */
    public int getPadValueSize() {
        return padBytes_;
    }

    /**
     * Field data for CDF record of type rVariable Descriptor Record.
     */
    public static class RVariant extends VariableDescriptorRecord {

        /**
         * Constructor.
         *
         * @param  plan  basic record info
         * @param  nameLeng  number of characters used for attribute names
         */
        public RVariant( RecordPlan plan, int nameLeng ) throws IOException {
            super( plan, "rVDR", 3, false, nameLeng );
        }
    }

    /**
     * Field data for CDF record of type zVariable Descriptor Record.
     */
    public static class ZVariant extends VariableDescriptorRecord {

        /**
         * Constructor.
         *
         * @param  plan  basic record info
         * @param  nameLeng  number of characters used for attribute names
         */
        public ZVariant( RecordPlan plan, int nameLeng ) throws IOException {
            super( plan, "zVDR", 8, true, nameLeng );
        }
    }
}
