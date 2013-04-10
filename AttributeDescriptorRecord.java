package cdf;

public class AttributeDescriptorRecord extends Record {

    public static int RECORD_TYPE = 4;

    private final long aedrNext_;
    private final long agrEdrHead_;
    private final int scope_;
    private final int num_;
    private final int ngrEntries_;
    private final int maxGrEntry_;
    private final int rfuA_;
    private final long azEdrHead_;
    private final int nzEntries_;
    private final int maxzEntry_;
    private final int rfuE_;
    private final String name_;

    public AttributeDescriptorRecord( long recSize, int recType,
                                      Buf buf, Offset offset12 ) {
        super( recSize, recType, RECORD_TYPE );
        Offset off = offset12;
        aedrNext_ = buf.readLong( off );
        agrEdrHead_ = buf.readLong( off );
        scope_ = buf.readInt( off );
        num_ = buf.readInt( off );
        ngrEntries_ = buf.readInt( off );
        maxGrEntry_ = buf.readInt( off );
        rfuA_ = checkIntValue( buf.readInt( off ), 0 );
        azEdrHead_ = buf.readLong( off );
        nzEntries_ = buf.readInt( off );
        maxzEntry_ = buf.readInt( off );
        rfuE_ = checkIntValue( buf.readInt( off ), -1 );
        name_ = buf.readAsciiString( off, 256 );
    }
}
