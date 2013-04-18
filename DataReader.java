package cdf;

import java.lang.reflect.Array;

public class DataReader {

    private final DataType dataType_;
    private final int count_;

    public DataReader( DataType dataType, int numElems, int nItem ) {
        dataType_ = dataType;
        count_ = numElems * nItem;
    }

    /**
     * Will return an array of a primitive type or String.
     */
    public Object createValueArray() {
        return Array.newInstance( dataType_.getArrayElementClass(), count_ );
    }

    public void readValue( Buf buf, long offset, Object valueArray ) {
        dataType_.readValues( buf, offset, valueArray, count_ );
    }

    public int getRecordSize() {
        return dataType_.getByteCount() * count_;
    }
}
