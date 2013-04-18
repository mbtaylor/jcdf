package cdf;

public interface Variable {
    String getName();
    int getNum();
    boolean isZVariable();
    int getMaxRec();
    DataReader getDataReader();
    boolean hasRecord( int irec );

    /**
     * Will return an array of a primitive type or String.
     */
    Object createRawValueArray();
    void readRawRecord( int irec, Object rawValueArray );
    Object readShapedRecord( int irec, boolean rowMajor,
                             Object rawValueArrayWorkspace );
}
