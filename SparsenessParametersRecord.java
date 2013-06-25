package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Sparseness Parameters Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class SparsenessParametersRecord extends Record {

    public final int sArraysType;
    public final int rfuA;
    public final int pCount;
    public final int[] sArraysParms;

    /**
     * Constructor.
     *
     * @param   plan  basic record information
     */
    public SparsenessParametersRecord( RecordPlan plan ) throws IOException {
        super( plan, "SPR", 12 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.sArraysType = buf.readInt( ptr );
        this.rfuA = checkIntValue( buf.readInt( ptr ), 0 );
        this.pCount = buf.readInt( ptr );
        this.sArraysParms = readIntArray( buf, ptr, this.pCount );
        checkEndRecord( ptr );
    }
}
