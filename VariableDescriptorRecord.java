package cdf;

public abstract class VariableDescriptorRecord extends Record {

    private final long vdrNext_;
    private final DataType dataType_;
    private final int maxRec_;
    private final long vxrHead_;
    private final long vxrTail_;
    private final int flags_;
    private final int sRecords_;
    private final int rfuB_;
    private final int rfuC_;
    private final int rfuF_;
    private final int numElems_;
    private final int num_;
    private final long cprOrSprOffset_;
    private final int blockingFactor_;
    private final String name_;
    private final int zNumDims_;
    private final int[] zDimSizes_;
    private final int[] dimVarys_;
    private final Object padValue_;

    private VariableDescriptorRecord( RecordPlan plan, int recordType,
                                      boolean hasDims ) {
        super( plan, recordType );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        vdrNext_ = buf.readOffset( ptr );
        dataType_ = getDataType( buf.readInt( ptr ) );
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
        int padSize = hasPad ? dataType_.getElementSize() : 0;
        final int ndim;
        if ( hasDims ) {
            ndim = zNumDims_;
        }
        else {

            // Work out the number of dimensions of an rVariable by subtracting
            // the values of all the other fields from the record size.
            // The more direct way would be by using the zNumDims field of
            // the GDR, but we don't have access to that here.
            long runningCount = plan.getReadCount( ptr );
            int padBytes = hasPad ? dataType_.getElementSize() : 0;
            long spareBytes = getRecordSize() - runningCount - padBytes;
            assert spareBytes == (int) spareBytes;
            if ( spareBytes % 4 != 0 ) {
                warnFormat( "rVDR DimVarys field non-integer size??" );
            }
            ndim = ( (int) spareBytes ) / 4;
        }
        dimVarys_ = readIntArray( buf, ptr, ndim );
        padValue_ = hasPad ? dataType_.readPadValue( buf, ptr ) : null;
        checkEndRecord( ptr );
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
