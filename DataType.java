package cdf;

import java.io.IOException;

import java.lang.reflect.Array;

/**
 * Enumerates the data types supported by the CDF format.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public abstract class DataType<A,S> {

    private final String name_;
    private final int byteCount_;
    private final int groupSize_;
    private final Class<A> arrayClass_;
    private final Class<S> scalarClass_;

    public static final DataType<byte[],Byte> INT1 =
        new Int1DataType( "INT1" );
    public static final DataType<short[],Short> INT2 =
        new Int2DataType( "INT2" );
    public static final DataType<int[],Integer> INT4 =
        new Int4DataType( "INT4" );
    public static final DataType<long[],Long> INT8 =
        new Int8DataType( "INT8" );
    public static final DataType<short[],Short> UINT1 =
        new UInt1DataType( "UINT1" );
    public static final DataType<int[],Integer> UINT2 =
        new UInt2DataType( "UINT2" );
    public static final DataType<long[],Long> UINT4 =
        new UInt4DataType( "UINT4" );
    public static final DataType<float[],Float> REAL4 =
        new Real4DataType( "REAL4" );
    public static final DataType<double[],Double> REAL8 =
        new Real8DataType( "REAL8" );
    public static final DataType<String[],String> CHAR =
        new CharDataType( "CHAR" );
    public static final DataType<double[],double[]> EPOCH16 =
        new Epoch16DataType( "EPOCH16" );
    public static final DataType<byte[],Byte> BYTE =
        new Int1DataType( "BYTE" );
    public static final DataType<float[],Float> FLOAT =
        new Real4DataType( "FLOAT" );
    public static final DataType<double[],Double> DOUBLE =
        new Real8DataType( "DOUBLE" );
    public static final DataType<double[],Double> EPOCH =
        new EpochDataType( "EPOCH" );
    public static final DataType<long[],Long> TIME_TT2000 =
        new Tt2kDataType( "TIME_TT2000");
    public static final DataType<String[],String> UCHAR =
        new CharDataType( "UCHAR" );
    
    /**
     * Constructor.
     *
     * @param  name  type name
     * @param  byteCount  number of bytes to store one item
     * @param  groupSize  number of elements that are read
     *                    into the value array for a single item read
     */
    private DataType( String name, int byteCount, int groupSize,
                      Class<A> arrayClass, Class<S> scalarClass ) {
        name_ = name;
        byteCount_ = byteCount;
        groupSize_ = groupSize;
        arrayClass_ = arrayClass;
        scalarClass_ = scalarClass;
    }

    /**
     * Returns the name for this data type.
     *
     * @return  data type name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the number of bytes used in a CDF to store a single item
     * of this type.
     *
     * @return  size in bytes
     */
    public int getByteCount() {
        return byteCount_;
    }

    /** 
     * Returns the class of an array that this data type can be read into.
     * In most cases this is an array of primitive types or Strings.
     *
     * @return   array raw value class
     */
    public Class<A> getArrayClass() {
        return arrayClass_;
    }

    /**
     * Returns the type of objects obtained by the <code>getScalar</code>
     * method.
     *
     * @return   scalar type associated with this data type
     */
    public Class<S> getScalarClass() {
        return scalarClass_;
    }

    /**
     * Number of elements that are read into
     * valueArray for a single item read.
     * This is usually 1, but not always, for instance for EPOCH16.
     *
     * @return   number of array elements per item
     */
    public int getGroupSize() {
        return groupSize_;
    }

    /**
     * Returns the index into a value array which corresponds to the
     * <code>item</code>'th element.
     *
     * @return   <code>itemIndex</code> * <code>groupSize</code>
     */
    public int getArrayIndex( int itemIndex ) {
        return groupSize_ * itemIndex;
    }

    /**
     * Reads data of this data type from a buffer into an appropriately
     * typed value array.
     *
     * @param   buf  data buffer
     * @param   offset  byte offset into buffer at which data starts
     * @param   numElems  number of elements per item;
     *                    usually 1, but may not be for strings
     * @param   valueArray  array to receive result data
     * @param   count  number of items to read
     */
    public abstract void readValues( Buf buf, long offset, int numElems,
                                     A valueArray, int count )
            throws IOException;

    /** 
     * Reads a single item from an array which has previously been
     * populated by {@link #readValues readValues}.
     *
     * <p>The <code>arrayIndex</code> argument is the index into the 
     * array object, not necessarily the item index -
     * see the {@link #getArrayIndex getArrayIndex} method.
     *
     * @param   valueArray  array filled with data for this data type
     * @param  arrayIndex  index into array at which the item to read is found
     * @return  scalar representation of object at position <code>index</code>
     *          in <code>valueArray</code>
     */
    public abstract S getScalar( A valueArray, int arrayIndex );

    /**
     * Provides a string view of a scalar value obtained for this data type.
     *
     * @param  value   value returned by <code>getScalar</code>
     * @return   string representation
     */
    public String formatScalarValue( S value ) {
        return value == null ? "" : value.toString();
    }

    /**
     * Provides a string view of an item obtained from an array value
     * of this data type.
     * <p>The <code>arrayIndex</code> argument is the index into the 
     * array object, not necessarily the item index -
     * see the {@link #getArrayIndex getArrayIndex} method.
     *
     * @param   array  array value populated by <code>readValues</code>
     * @param   arrayIndex  index into array
     * @return  string representation
     */
    public String formatArrayValue( A array, int arrayIndex ) {
        Object value = Array.get( array, arrayIndex );
        return value == null ? "" : value.toString();
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the DataType object corresponding to a CDF data type code.
     *
     * @param  dataType  dataType field of AEDR or VDR
     * @return   data type object
     */
    public static DataType getDataType( int dataType )
            throws CdfFormatException {
        switch ( dataType ) {
            case  1: return INT1;
            case  2: return INT2;
            case  4: return INT4;
            case  8: return INT8;
            case 11: return UINT1;
            case 12: return UINT2;
            case 14: return UINT4;
            case 41: return BYTE;
            case 21: return REAL4;
            case 22: return REAL8;
            case 44: return FLOAT;
            case 45: return DOUBLE;
            case 31: return EPOCH;
            case 32: return EPOCH16;
            case 33: return TIME_TT2000;
            case 51: return CHAR;
            case 52: return UCHAR;
            default:
                throw new CdfFormatException( "Unknown data type " + dataType );
        }
    }

    /**
     * DataType for signed 1-byte integer.
     */
    private static final class Int1DataType extends DataType<byte[],Byte> {
        Int1DataType( String name ) {
            super( name, 1, 1, byte[].class, Byte.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                byte[] array, int n ) throws IOException {
            buf.readDataBytes( offset, n, array );
        }
        public Byte getScalar( byte[] array, int index ) {
            return new Byte( array[ index ] );
        }
    }

    /**
     * DataType for signed 2-byte integer.
     */
    private static final class Int2DataType extends DataType<short[],Short> {
        Int2DataType( String name ) {
            super( name, 2, 1, short[].class, Short.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                short[] array, int n ) throws IOException {
            buf.readDataShorts( offset, n, array );
        }
        public Short getScalar( short[] array, int index ) {
            return new Short( array[ index ] );
        }
    }

    /**
     * DataType for signed 4-byte integer.
     */
    private static final class Int4DataType extends DataType<int[],Integer> {
        Int4DataType( String name ) {
            super( name, 4, 1, int[].class, Integer.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                int[] array, int n ) throws IOException {
            buf.readDataInts( offset, n, array );
        }
        public Integer getScalar( int[] array, int index ) {
            return new Integer( array[ index ] );
        }
    }

    /**
     * DataType for signed 8-byte integer.
     */
    private static class Int8DataType extends DataType<long[],Long> {
        Int8DataType( String name ) {
            super( name, 8, 1, long[].class, Long.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                long[] array, int n ) throws IOException {
            buf.readDataLongs( offset, n, array );
        }
        public Long getScalar( long[] array, int index ) {
            return new Long( array[ index ] );
        }
    }

    /**
     * DataType for unsigned 1-byte integer.
     * Output values are 2-byte signed integers because of the difficulty
     * of handling unsigned integers in java.
     */
    private static class UInt1DataType extends DataType<short[],Short> {
        UInt1DataType( String name ) {
            super( name, 1, 1, short[].class, Short.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                short[] array, int n ) throws IOException {
            Pointer ptr = new Pointer( offset );
            for ( int i = 0; i < n; i++ ) {
                array[ i ] = (short) buf.readUnsignedByte( ptr );
            }
        }
        public Short getScalar( short[] array, int index ) {
            return new Short( array[ index ] );
        }
    }

    /**
     * DataType for unsigned 2-byte integer.
     * Output vaules are 4-byte signed integers because of the difficulty
     * of handling unsigned integers in java.
     */
    private static class UInt2DataType extends DataType<int[],Integer> {
        UInt2DataType( String name ) {
            super( name, 2, 1, int[].class, Integer.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                int[] array, int n ) throws IOException {
            Pointer ptr = new Pointer( offset );
            boolean bigend = buf.isBigendian();
            for ( int i = 0; i < n; i++ ) {
                int b0 = buf.readUnsignedByte( ptr );
                int b1 = buf.readUnsignedByte( ptr );
                array[ i ] = bigend ? b1 | ( b0 << 8 )
                                    : b0 | ( b1 << 8 );
            }
        }
        public Integer getScalar( int[] array, int index ) {
            return new Integer( array[ index ] );
        }
    }

    /** 
     * DataType for unsigned 4-byte integer.
     * Output values are 8-byte signed integers because of the difficulty
     * of handling unsigned integers in java.
     */
    private static class UInt4DataType extends DataType<long[],Long> {
        UInt4DataType( String name ) {
            super( name, 4, 1, long[].class, Long.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                long[] array, int n ) throws IOException {
            Pointer ptr = new Pointer( offset );
            boolean bigend = buf.isBigendian();
            for ( int i = 0; i < n; i++ ) {
                long b0 = buf.readUnsignedByte( ptr );
                long b1 = buf.readUnsignedByte( ptr );
                long b2 = buf.readUnsignedByte( ptr );
                long b3 = buf.readUnsignedByte( ptr );
                array[ i ] = bigend
                           ? b3 | ( b2 << 8 ) | ( b1 << 16 ) | ( b0 << 24 )
                           : b0 | ( b1 << 8 ) | ( b2 << 16 ) | ( b3 << 24 );
            }
        }
        public Long getScalar( long[] array, int index ) {
            return new Long( ((long[]) array )[ index ] );
        }
    }

    /**
     * DataType for 4-byte floating point.
     */
    private static class Real4DataType extends DataType<float[],Float> {
        Real4DataType( String name ) {
            super( name, 4, 1, float[].class, Float.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                float[] array, int n ) throws IOException {
            buf.readDataFloats( offset, n, array );
        }
        public Float getScalar( float[] array, int index ) {
            return new Float( array[ index ] );
        }
    }

    /**
     * DataType for 8-byte floating point.
     */
    private static class Real8DataType extends DataType<double[],Double> {
        Real8DataType( String name ) {
            super( name, 8, 1, double[].class, Double.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                double[] array, int n ) throws IOException {
            buf.readDataDoubles( offset, n, array );
        }
        public Double getScalar( double[] array, int index ) {
            return new Double( array[ index ] );
        }
    }

    /**
     * DataType for TIME_TT2000.
     */
    private static class Tt2kDataType extends Int8DataType {
        final EpochFormatter formatter_ = new EpochFormatter();
        Tt2kDataType( String name ) {
            super( name );
        }
        @Override
        public String formatScalarValue( Long value ) {
            synchronized ( formatter_ ) {
                return formatter_.formatTimeTt2000( value.longValue() );
            }
        }
        @Override
        public String formatArrayValue( long[] array, int index ) {
            synchronized ( formatter_ ) {
                return formatter_.formatTimeTt2000( array[ index ] );
            }
        }
    }

    /**
     * DataType for 1-byte character.
     * Output is as numElem-character String.
     */
    private static class CharDataType extends DataType<String[],String> {
        CharDataType( String name ) {
            super( name, 1, 1, String[].class, String.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                String[] array, int n ) throws IOException {
            byte[] cbuf = new byte[ numElems * n ];
            buf.readDataBytes( offset, numElems * n, cbuf );
            for ( int i = 0; i < n; i++ ) {
                @SuppressWarnings("deprecation")
                String s = new String( cbuf, i * numElems, numElems );
                array[ i ] = s;
            }
        }
        public String getScalar( String[] array, int index ) {
            return array[ index ];
        }
    }

    /**
     * DataType for 8-byte floating point epoch.
     */
    private static class EpochDataType extends Real8DataType {
        private final EpochFormatter formatter_ = new EpochFormatter();
        EpochDataType( String name ) {
            super( name );
        }
        @Override
        public String formatScalarValue( Double value ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch( value.doubleValue() );
            }
        }
        @Override
        public String formatArrayValue( double[] array, int index ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch( array[ index ] );
            }
        }
    }

    /**
     * DataType for 16-byte (2*double) epoch.
     * Output is as a 2-element array of doubles.
     */
    private static class Epoch16DataType extends DataType<double[],double[]> {
        private final EpochFormatter formatter_ = new EpochFormatter();
        Epoch16DataType( String name ) {
            super( name, 8, 2, double[].class, double[].class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                double[] array, int n ) throws IOException {
            buf.readDataDoubles( offset, n * 2, array );
        }
        public double[] getScalar( double[] array, int index ) {
            return new double[] { array[ index ], array[ index + 1 ] };
        }
        @Override
        public String formatScalarValue( double[] v2 ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch16( v2[ 0 ], v2[ 1 ] );
            }
        }
        @Override
        public String formatArrayValue( double[] array, int index ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch16( array[ index ],
                                                 array[ index + 1 ] );
            }
        }
    }
}
