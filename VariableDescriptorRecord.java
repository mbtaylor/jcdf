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
    public final boolean[] dimVarys_;
    private final long padOffset_;

    private VariableDescriptorRecord( RecordPlan plan, int recordType,
                                      boolean hasDims, int nameLeng ) {
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
        int padBytes = hasPad ? DataType.getDataType( dataType_ ).getByteCount()
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
            long spareBytes = getRecordSize() - runningCount - padBytes;
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
        padOffset_ = hasPad ? ptr.get() : -1L;
        ptr.increment( padBytes );
        checkEndRecord( ptr );
    }

    /**
     * Will be -1 if no pad.
     */
    public long getPadOffset() {
        return padOffset_;
    }

    public static class RVariant extends VariableDescriptorRecord {
        public RVariant( RecordPlan plan, int nameLeng ) {
            super( plan, 3, false, nameLeng );
        }
    }

    public static class ZVariant extends VariableDescriptorRecord {
        public ZVariant( RecordPlan plan, int nameLeng ) {
            super( plan, 8, true, nameLeng );
        }
    }
}
