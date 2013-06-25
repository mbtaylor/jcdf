package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Sparseness Parameters Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class SparsenessParametersRecord extends Record {

    public final int sArraysType_;
    public final int rfuA_;
    public final int pCount_;
    public final int[] sArraysParms_;

    /**
     * Constructor.
     *
     * @param   plan  basic record information
     */
    public SparsenessParametersRecord( RecordPlan plan ) throws IOException {
        super( plan, "SPR", 12 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        sArraysType_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        pCount_ = buf.readInt( ptr );
        sArraysParms_ = readIntArray( buf, ptr, pCount_ );
        checkEndRecord( ptr );
    }
}
