package cdf;

public abstract class DataType {

    public abstract int getByteCount();
    public abstract Class<?> getArrayElementClass();
    public abstract Class<?> getScalarClass();
    public abstract void readValues( Buf buf, long offset, Object valueArray,
                                     int count );

    /** Index is the index into the array. */
    public abstract Object readScalar( Object valueArray, int index,
                                       int numElems );

    public static DataType getDataType( int dtype ) {
  return null;
    }
}
