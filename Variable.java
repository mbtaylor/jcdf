package cdf;

public interface Variable {
    String getName();
    int getNum();
    boolean isZVariable();

    /**
     * Limit of records that may have values.
     * The actual number of records may be lower than this in case of sparsity.
     */
    int getRecordCount();
    DataReader getDataReader();
    Shaper getShaper();

    /**
     * Whether a real distinct file-based record exists for the given index.
     * Reading a record will give you a result in any case, but if this
     * returns false it will be some kind of fixed or default value.
     */
    boolean hasRecord( int irec );

    /**
     * Will return an array of a primitive type or String.
     */
    Object createRawValueArray();
    void readRawRecord( int irec, Object rawValueArray );
    Object readShapedRecord( int irec, boolean rowMajor,
                             Object rawValueArrayWorkspace );
}
