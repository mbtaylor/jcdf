package cdf;

public interface DataReader {
    Object readRawValue( Buf buf, Pointer ptr );
    Object readShapedValue( Buf buf, Pointer ptr, boolean rowMajor );
    int getRecordSize();
}
