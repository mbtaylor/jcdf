package uk.ac.bristol.star.cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Attribute Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class AttributeDescriptorRecord extends Record {

    @CdfField @OffsetField public final long adrNext;
    @CdfField @OffsetField public final long agrEdrHead;
    @CdfField public final int scope;
    @CdfField public final int num;
    @CdfField public final int nGrEntries;
    @CdfField public final int maxGrEntry;
    @CdfField public final int rfuA;
    @CdfField @OffsetField public final long azEdrHead;
    @CdfField public final int nZEntries;
    @CdfField public final int maxZEntry;
    @CdfField public final int rfuE;
    @CdfField public final String name;

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
        this.rfuA = buf.readInt( ptr );
        this.azEdrHead = buf.readOffset( ptr );
        this.nZEntries = buf.readInt( ptr );
        this.maxZEntry = buf.readInt( ptr );
        this.rfuE = buf.readInt( ptr );
        this.name = buf.readAsciiString( ptr, nameLeng );
        checkEndRecord( ptr );
    }
}
