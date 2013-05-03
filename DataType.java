package cdf;

import java.lang.reflect.Array;

public abstract class DataType {

    private final String name_;
    private final int byteCount_;
    private final int groupSize_;
    private final Class<?> arrayElementClass_;
    private final Class<?> scalarClass_;

    public static final DataType INT1 = new Int1DataType( "INT1" );
    public static final DataType INT2 = new Int2DataType( "INT2" );
    public static final DataType INT4 = new Int4DataType( "INT4" );
    public static final DataType INT8 = new Int8DataType( "INT8" );
    public static final DataType UINT1 = new UInt1DataType( "UINT1" );
    public static final DataType UINT2 = new UInt2DataType( "UINT2" );
    public static final DataType UINT4 = new UInt4DataType( "UINT4" );
    public static final DataType REAL4 = new Real4DataType( "REAL4" );
    public static final DataType REAL8 = new Real8DataType( "REAL8" );
    public static final DataType CHAR = new CharDataType( "CHAR" );
    public static final DataType EPOCH16 = new Epoch16DataType( "EPOCH16" );
    public static final DataType BYTE = new Int1DataType( "BYTE" );
    public static final DataType FLOAT = new Real4DataType( "FLOAT" );
    public static final DataType DOUBLE = new Real8DataType( "DOUBLE" );
    public static final DataType EPOCH = new EpochDataType( "EPOCH" );
    public static final DataType TIME_TT2000 = new Tt2kDataType( "TIME_TT2000");
    public static final DataType UCHAR = new CharDataType( "UCHAR" );
    
    private static final DataType[] TYPE_TABLE = createTypeTable();

    private DataType( String name, int byteCount, int groupSize,
                      Class<?> arrayElementClass, Class<?> scalarClass ) {
        name_ = name;
        byteCount_ = byteCount;
        groupSize_ = groupSize;
        arrayElementClass_ = arrayElementClass;
        scalarClass_ = scalarClass;
    }

    public String getName() {
        return name_;
    }

    public int getByteCount() {
        return byteCount_;
    }

    public Class<?> getArrayElementClass() {
        return arrayElementClass_;
    }

    public Class<?> getScalarClass() {
        return scalarClass_;
    }

    public String formatScalarValue( Object value ) {
        return value.toString();
    }

    public String formatArrayValue( Object array, int index ) {
        return Array.get( array, index ).toString();
    }

    /**
     * Number of elements of type arrayElementClass that are read into
     * valueArray for a single item read.
     */
    public int getGroupSize() {
        return groupSize_;
    }

    public abstract void readValues( Buf buf, long offset, int numElems,
                                     Object valueArray, int count );

    /** Index is the index into the array. */
    public abstract Object getScalar( Object valueArray, int index );

    public static DataType getDataType( int dtype ) {
        DataType dataType = dtype >= 0 && dtype < TYPE_TABLE.length
                          ? TYPE_TABLE[ dtype ]
                          : null;
        if ( dataType != null ) {
            return dataType;
        }
        else {
            throw new CdfFormatException( "Unknown data type " + dtype );
        }
    }

    private static final DataType[] createTypeTable() {
        DataType[] table = new DataType[ 53 ];
        table[ 1 ] = INT1;
        table[ 2 ] = INT2;
        table[ 4 ] = INT4;
        table[ 8 ] = INT8;
        table[ 11 ] = UINT1;
        table[ 12 ] = UINT2;
        table[ 14 ] = UINT4;
        table[ 41 ] = BYTE;
        table[ 21 ] = REAL4;
        table[ 22 ] = REAL8;
        table[ 44 ] = FLOAT;
        table[ 45 ] = DOUBLE;
        table[ 31 ] = EPOCH;
        table[ 32 ] = EPOCH16;
        table[ 33 ] = TIME_TT2000;
        table[ 51 ] = CHAR;
        table[ 52 ] = UCHAR;
        return table;
    }

    private static final class Int1DataType extends DataType {
        Int1DataType( String name ) {
            super( name, 1, 1, byte.class, Byte.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataBytes( offset, n, (byte[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Byte( ((byte[]) array)[ index ] );
        }
    }

    private static final class Int2DataType extends DataType {
        Int2DataType( String name ) {
            super( name, 2, 1, short.class, Short.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataShorts( offset, n, (short[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Short( ((short[]) array)[ index ] );
        }
    }

    private static final class Int4DataType extends DataType {
        Int4DataType( String name ) {
            super( name, 4, 1, int.class, Integer.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataInts( offset, n, (int[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Integer( ((int[]) array)[ index ] );
        }
    }

    private static class Int8DataType extends DataType {
        Int8DataType( String name ) {
            super( name, 8, 1, long.class, Long.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataLongs( offset, n, (long[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Long( ((long[]) array)[ index ] );
        }
    }

    private static class UInt1DataType extends DataType {
        UInt1DataType( String name ) {
            super( name, 1, 1, short.class, Short.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            Pointer ptr = new Pointer( offset );
            short[] sarray = (short[]) array;
            for ( int i = 0; i < n; i++ ) {
                sarray[ i ] = (short) buf.readUnsignedByte( ptr );
            }
        }
        public Object getScalar( Object array, int index ) {
            return new Short( ((short[]) array)[ index ] );
        }
    }

    private static class UInt2DataType extends DataType {
        UInt2DataType( String name ) {
            super( name, 2, 1, int.class, Integer.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            Pointer ptr = new Pointer( offset );
            int[] iarray = (int[]) array;
            boolean bigend = buf.isBigendian();
            for ( int i = 0; i < n; i++ ) {
                int b0 = buf.readUnsignedByte( ptr );
                int b1 = buf.readUnsignedByte( ptr );
                iarray[ i ] = bigend ? b1 | ( b0 << 8 )
                                     : b0 | ( b1 << 8 );
            }
        }
        public Object getScalar( Object array, int index ) {
            return new Integer( ((int[]) array)[ index ] );
        }
    }

    private static class UInt4DataType extends DataType {
        UInt4DataType( String name ) {
            super( name, 4, 1, long.class, Long.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            Pointer ptr = new Pointer( offset );
            long[] larray = (long[]) array;
            boolean bigend = buf.isBigendian();
            for ( int i = 0; i < n; i++ ) {
                long b0 = buf.readUnsignedByte( ptr );
                long b1 = buf.readUnsignedByte( ptr );
                long b2 = buf.readUnsignedByte( ptr );
                long b3 = buf.readUnsignedByte( ptr );
                larray[ i ] = bigend
                            ? b3 | ( b2 << 8 ) | ( b1 << 16 ) | ( b0 << 24 )
                            : b0 | ( b1 << 8 ) | ( b2 << 16 ) | ( b3 << 24 );
            }
        }
        public Object getScalar( Object array, int index ) {
            return new Long( ((long[]) array )[ index ] );
        }
    }

    private static class Real4DataType extends DataType {
        Real4DataType( String name ) {
            super( name, 4, 1, float.class, Float.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataFloats( offset, n, (float[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Float( ((float[]) array)[ index ] );
        }
    }

    private static class Real8DataType extends DataType {
        Real8DataType( String name ) {
            super( name, 8, 1, double.class, Double.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataDoubles( offset, n, (double[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Double( ((double[]) array)[ index ] );
        }
    }

    private static class Tt2kDataType extends Int8DataType {
        final EpochFormatter formatter_ = new EpochFormatter();
        Tt2kDataType( String name ) {
            super( name );
        }
        @Override
        public String formatScalarValue( Object value ) {
            synchronized ( formatter_ ) {
                return formatter_
                      .formatTimeTt2000( ((Long) value).longValue() );
            }
        }
        @Override
        public String formatArrayValue( Object array, int index ) {
            synchronized ( formatter_ ) {
                return formatter_
                      .formatTimeTt2000( ((long[]) array)[ index ] );
            }
        }
    }

    private static class CharDataType extends DataType {
        CharDataType( String name ) {
            super( name, 1, 1, String.class, String.class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            String[] sarray = (String[]) array;
            byte[] cbuf = new byte[ numElems ];
            for ( int i = 0; i < n; i++ ) {
                buf.readDataBytes( offset, numElems, cbuf );
                @SuppressWarnings("deprecation")
                String s = new String( cbuf, 0 );
                sarray[ i ] = s;
            }
        }
        public Object getScalar( Object array, int index ) {
            return ((String[]) array)[ index ];
        }
    }

    private static class EpochDataType extends Real8DataType {
        private final EpochFormatter formatter_ = new EpochFormatter();
        EpochDataType( String name ) {
            super( name );
        }
        @Override
        public String formatScalarValue( Object value ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch( ((Double) value).doubleValue() );
            }
        }
        @Override
        public String formatArrayValue( Object array, int index ) {
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch( ((double[]) array)[ index ] );
            }
        }
    }

    private static class Epoch16DataType extends DataType {
        private final EpochFormatter formatter_ = new EpochFormatter();
        Epoch16DataType( String name ) {
            super( name, 8, 2, double.class, double[].class );
        }
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataDoubles( offset, n * 2, (double[]) array );
        }
        public Object getScalar( Object array, int index ) {
            double[] darray = (double[]) array;
            return new double[] { darray[ index ], darray[ index + 1 ] };
        }
        @Override
        public String formatScalarValue( Object value ) {
            double[] v2 = (double[]) value;
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch16( v2[ 0 ], v2[ 1 ] );
            }
        }
        @Override
        public String formatArrayValue( Object array, int index ) {
            double[] darray = (double[]) array;
            synchronized ( formatter_ ) {
                return formatter_.formatEpoch16( darray[ index ],
                                                 darray[ index + 1 ] );
            }
        }
    }
}
