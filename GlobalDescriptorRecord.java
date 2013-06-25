package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Global Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class GlobalDescriptorRecord extends Record {

    public final long rVdrHead;
    public final long zVdrHead;
    public final long adrHead;
    public final long eof;
    public final int nrVars;
    public final int numAttr;
    public final int rMaxRec;
    public final int rNumDims;
    public final int nzVars;
    public final long uirHead;
    public final int rfuC;
    public final int rfuD;
    public final int rfuE;
    public final int[] rDimSizes;

    /**
     * Constructor.
     *
     * @param  plan   basic record information
     */
    public GlobalDescriptorRecord( RecordPlan plan ) throws IOException {
        super( plan, "GDR", 2 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.rVdrHead = buf.readOffset( ptr );
        this.zVdrHead = buf.readOffset( ptr );
        this.adrHead = buf.readOffset( ptr );
        this.eof = buf.readOffset( ptr );
        this.nrVars = buf.readInt( ptr );
        this.numAttr = buf.readInt( ptr );
        this.rMaxRec = buf.readInt( ptr );
        this.rNumDims = buf.readInt( ptr );
        this.nzVars = buf.readInt( ptr );
        this.uirHead = buf.readOffset( ptr );
        this.rfuC = checkIntValue( buf.readInt( ptr ), 0 );
        this.rfuD = checkIntValue( buf.readInt( ptr ), -1 );
        this.rfuE = checkIntValue( buf.readInt( ptr ), -1 );
        this.rDimSizes = readIntArray( buf , ptr, this.rNumDims );
        checkEndRecord( ptr );
    }
}
