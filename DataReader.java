package cdf;

public interface DataReader {

    /**
     * Will return an array of a primitive type or String.
     */
    Object createValueArray();
    void readValue( Buf buf, long offset, Object valueArray );
    int getRecordSize();
}
