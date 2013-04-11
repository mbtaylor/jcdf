package cdf;

public interface DataType {
    int getCode();
    int getElementSize();
    Object readPadValue( Buf buf, Pointer ptr );
}
