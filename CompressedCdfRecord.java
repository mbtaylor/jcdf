package uk.ac.bristol.star.cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Compressed CDF Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CompressedCdfRecord extends Record {

    @CdfField @OffsetField public final long cprOffset;
    @CdfField public final long uSize;
    @CdfField public final int rfuA;
    private final long dataOffset_;

    /**
     * Constructor.
     *
     * @param  plan  basic record information
     */
    public CompressedCdfRecord( RecordPlan plan ) throws IOException {
        super( plan, "CCR", 10 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.cprOffset = buf.readOffset( ptr );
        this.uSize = buf.readOffset( ptr );
        this.rfuA = buf.readInt( ptr );
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
