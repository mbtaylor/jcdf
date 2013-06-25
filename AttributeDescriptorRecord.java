package cdf.record;

import java.io.IOException;

/**
 * Field data for CDF record of type Attribute Descriptor Record.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class AttributeDescriptorRecord extends Record {

    public final long adrNext_;
    public final long agrEdrHead_;
    public final int scope_;
    public final int num_;
    public final int nGrEntries_;
    public final int maxGrEntry_;
    public final int rfuA_;
    public final long azEdrHead_;
    public final int nZEntries_;
    public final int maxZEntry_;
    public final int rfuE_;
    public final String name_;

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
        adrNext_ = buf.readOffset( ptr );
        agrEdrHead_ = buf.readOffset( ptr );
        scope_ = buf.readInt( ptr );
        num_ = buf.readInt( ptr );
        nGrEntries_ = buf.readInt( ptr );
        maxGrEntry_ = buf.readInt( ptr );
        rfuA_ = checkIntValue( buf.readInt( ptr ), 0 );
        azEdrHead_ = buf.readOffset( ptr );
        nZEntries_ = buf.readInt( ptr );
        maxZEntry_ = buf.readInt( ptr );
        rfuE_ = checkIntValue( buf.readInt( ptr ), -1 );
        name_ = buf.readAsciiString( ptr, nameLeng );
        checkEndRecord( ptr );
    }
}
