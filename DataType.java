package cdf;

public abstract class DataType {

    public abstract int getByteCount();
    public abstract Class<?> getArrayElementClass();
    public abstract Class<?> getScalarClass();

    /**
     * Number of elements of type arrayElementClass that are read into
     * valueArray for a single item read.
     */
    public abstract int getGroupSize();
    public abstract void readValues( Buf buf, long offset, Object valueArray,
                                     int count );

    /** Index is the index into the array. */
    public abstract Object getScalar( Object valueArray, int index,
                                      int numElems );

    public static DataType getDataType( int dtype ) {
  return null;
    }
}
