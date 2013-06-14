package cdf;

import java.io.IOException;
import java.lang.reflect.Array;

public class DataReader {

    private final DataType dataType_;
    private final int numElems_;
    private final int nItem_;

    public DataReader( DataType dataType, int numElems, int nItem ) {
        dataType_ = dataType;
        numElems_ = numElems;
        nItem_ = nItem;
    }

    /**
     * Will return an array of a primitive type or String.
     */
    public Object createValueArray() {
        return Array.newInstance( dataType_.getArrayElementClass(), nItem_ );
    }

    public void readValue( Buf buf, long offset, Object valueArray )
            throws IOException {
        dataType_.readValues( buf, offset, numElems_, valueArray, nItem_ );
    }

    public int getRecordSize() {
        return dataType_.getByteCount() * numElems_ * nItem_;
    }
}
