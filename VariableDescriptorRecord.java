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

    public final long vdrNext;
    public final int dataType;
    public final int maxRec;
    public final long vxrHead;
    public final long vxrTail;
    public final int flags;
    public final int sRecords;
    public final int rfuB;
    public final int rfuC;
    public final int rfuF;
    public final int numElems;
    public final int num;
    public final long cprOrSprOffset;
    public final int blockingFactor;
    public final String name;
    public final int zNumDims;
    public final int[] zDimSizes;
    public final boolean[] dimVarys;
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
        this.vdrNext = buf.readOffset( ptr );
        this.dataType = buf.readInt( ptr );
        this.maxRec = buf.readInt( ptr );
        this.vxrHead = buf.readOffset( ptr );
        this.vxrTail = buf.readOffset( ptr );
        this.flags = buf.readInt( ptr );
        this.sRecords = buf.readInt( ptr );
        this.rfuB = checkIntValue( buf.readInt( ptr ), 0 );
        this.rfuC = checkIntValue( buf.readInt( ptr ), -1 );
        this.rfuF = checkIntValue( buf.readInt( ptr ), -1 );
        this.numElems = buf.readInt( ptr );
        this.num = buf.readInt( ptr );
        this.cprOrSprOffset = buf.readOffset( ptr );
        this.blockingFactor = buf.readInt( ptr );
        this.name = buf.readAsciiString( ptr, nameLeng );
        if ( hasDims ) {
            this.zNumDims = buf.readInt( ptr );
            this.zDimSizes = readIntArray( buf, ptr, this.zNumDims );
        }
        else {
            this.zNumDims = 0;
            this.zDimSizes = null;
        }
        boolean hasPad = hasBit( this.flags, 1 );
        padBytes_ = hasPad ? DataType.getDataType( this.dataType )
                                     .getByteCount() * this.numElems
                           : 0;
        final int ndim;
        if ( hasDims ) {
            ndim = this.zNumDims;
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
        this.dimVarys = new boolean[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            this.dimVarys[ i ] = iDimVarys[ i ] != 0;
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
