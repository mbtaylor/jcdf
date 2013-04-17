package cdf;

public interface DataReader {
    Object readValue( Buf buf, long offset );
    int getRecordSize();
}
