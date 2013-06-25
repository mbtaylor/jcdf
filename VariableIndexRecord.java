package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Variable Index Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class VariableIndexRecord extends Record {

    public final long vxrNext;
    public final int nEntries;
    public final int nUsedEntries;
    public final int[] first;
    public final int[] last;
    public final long[] offset;

    /** 
     * Constructor.
     *
     * @param  plan   basic record information
     */
    public VariableIndexRecord( RecordPlan plan ) throws IOException {
        super( plan, "VXR", 6 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.vxrNext = buf.readOffset( ptr );
        this.nEntries = buf.readInt( ptr );
        this.nUsedEntries = buf.readInt( ptr );
        this.first = readIntArray( buf, ptr, this.nEntries );
        this.last = readIntArray( buf, ptr, this.nEntries );
        this.offset = readOffsetArray( buf, ptr, this.nEntries );
        checkEndRecord( ptr );
    }
}
