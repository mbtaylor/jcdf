package cdf;

public interface DataReader {
    Object readRawValue( Buf buf, long offset );
    Object readShapedValue( Buf buf, long offset, boolean rowMajor );
    int getRecordSize();
}
