package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Compressed CDF Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CompressedCdfRecord extends Record {

    public final long cprOffset_;
    public final long uSize_;
    public final int rfuA_;
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
        cprOffset_ = buf.readOffset( ptr );
        uSize_ = buf.readOffset( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
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
