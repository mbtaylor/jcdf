package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Attribute Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class AttributeDescriptorRecord extends Record {

    public final long adrNext;
    public final long agrEdrHead;
    public final int scope;
    public final int num;
    public final int nGrEntries;
    public final int maxGrEntry;
    public final int rfuA;
    public final long azEdrHead;
    public final int nZEntries;
    public final int maxZEntry;
    public final int rfuE;
    public final String name;

    /**
     * Constructor.
     *
     * @param  plan  basic record info
     * @param  nameLeng  number of characters used for attribute names
     */
    public AttributeDescriptorRecord( RecordPlan plan, int nameLeng )
            throws IOException {
        super( plan, "ADR", 4 );
        Buf buf = plan.getBuf();
        Pointer ptr = plan.createContentPointer();
        this.adrNext = buf.readOffset( ptr );
        this.agrEdrHead = buf.readOffset( ptr );
        this.scope = buf.readInt( ptr );
        this.num = buf.readInt( ptr );
        this.nGrEntries = buf.readInt( ptr );
        this.maxGrEntry = buf.readInt( ptr );
        this.rfuA = checkIntValue( buf.readInt( ptr ), 0 );
        this.azEdrHead = buf.readOffset( ptr );
        this.nZEntries = buf.readInt( ptr );
        this.maxZEntry = buf.readInt( ptr );
        this.rfuE = checkIntValue( buf.readInt( ptr ), -1 );
        this.name = buf.readAsciiString( ptr, nameLeng );
        checkEndRecord( ptr );
    }
}
