package cdf;

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
    public final int[] dimVarys_;
    private final long padOffset_;

    private VariableDescriptorRecord( RecordPlan plan, int recordType,
                                      boolean hasDims ) {
        super( plan, recordType );
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
        name_ = buf.readAsciiString( ptr, 256 );
        if ( hasDims ) {
            zNumDims_ = buf.readInt( ptr );
            zDimSizes_ = readIntArray( buf, ptr, zNumDims_ );
        }
        else {
            zNumDims_ = 0;
            zDimSizes_ = null;
        }
        boolean hasPad = hasBit( flags_, 1 );
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
            int padBytes =
                hasPad ? DataReaderFactory.getElementSize( dataType_ )
                         * numElems_
                       : 0;
            long spareBytes = getRecordSize() - runningCount - padBytes;
            assert spareBytes == (int) spareBytes;
            if ( spareBytes % 4 != 0 ) {
                warnFormat( "rVDR DimVarys field non-integer size??" );
            }
            ndim = ( (int) spareBytes ) / 4;
        }
        dimVarys_ = readIntArray( buf, ptr, ndim );
        padOffset_ = hasPad ? ptr.get() : -1L;
        checkEndRecord( ptr );
    }

    /**
     * Will be -1 if no pad.
     */
    public long getPadOffset() {
        return padOffset_;
    }

    public static class RVariant extends VariableDescriptorRecord {
        public static final int RECORD_TYPE = 3;
        public RVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE, false );
        }
    }

    public static class ZVariant extends VariableDescriptorRecord {
        public static final int RECORD_TYPE = 8;
        public ZVariant( RecordPlan plan ) {
            super( plan, RECORD_TYPE, true );
        }
    }
}
