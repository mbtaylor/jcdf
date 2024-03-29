package uk.ac.bristol.star.cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Compressed Variable Values Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CompressedVariableValuesRecord extends Record {

    @CdfField public final int rfuA;
    @CdfField public final long cSize;
    private final long dataOffset_;

    /**
     * Constructor.
     *
     * @param  plan   basic record information
     */
    public CompressedVariableValuesRecord( RecordPlan plan )
            throws IOException {
        super( plan, "CVVR", 13 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.rfuA = buf.readInt( ptr );
        this.cSize = buf.readOffset( ptr );
        dataOffset_ = ptr.get();
    }

    /**
     * Returns the file offset at which the compressed data in
     * this record starts.
     *
     * @return  file offset for start of data field
     */
    public long getDataOffset() {
        return dataOffset_;
    }
}
