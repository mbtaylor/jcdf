package cdf;

public interface Variable {
    String getName();
    int getNum();
    boolean isZVariable();
    int getMaxRec();
    DataReader getDataReader();
    boolean hasRecord( int irec );
    Object readRawRecord( int irec );
    Object readShapedRecord( int irec, boolean rowMajor );
}
