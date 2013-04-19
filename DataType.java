package cdf;

public abstract class DataType {

    private final int byteCount_;
    private final int groupSize_;
    private final Class<?> arrayElementClass_;
    private final Class<?> scalarClass_;

    public static final DataType INT1 =
            new DataType( 1, 1, byte.class, Byte.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataBytes( offset, n, (byte[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Byte( ((byte[]) array)[ index ] );
        }
    };
    public static final DataType INT2 =
            new DataType( 2, 1, short.class, Short.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataShorts( offset, n, (short[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Short( ((short[]) array)[ index ] );
        }
    };
    public static final DataType INT4 =
            new DataType( 4, 1, int.class, Integer.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataInts( offset, n, (int[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Integer( ((int[]) array)[ index ] );
        }
    };
    public static final DataType INT8 =
            new DataType( 8, 1, long.class, Long.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataLongs( offset, n, (long[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Long( ((long[]) array)[ index ] );
        }
    };
    public static final DataType UINT1 =
            new DataType( 1, 1, short.class, Short.class ) {
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
    };
    public static final DataType UINT2 =
            new DataType( 2, 1, int.class, Integer.class ) {
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
    };
    public static final DataType UINT4 =
            new DataType( 4, 1, long.class, Long.class ) {
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
    };
    public static final DataType REAL4 =
            new DataType( 4, 1, float.class, Float.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataFloats( offset, n, (float[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Float( ((float[]) array)[ index ] );
        }
    };
    public static final DataType REAL8 =
            new DataType( 8, 1, double.class, Double.class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataDoubles( offset, n, (double[]) array );
        }
        public Object getScalar( Object array, int index ) {
            return new Double( ((double[]) array)[ index ] );
        }
    };
    public static final DataType CHAR =
            new DataType( 1, 1, String.class, String.class ) {
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
    };
    public static final DataType EPOCH16 =
            new DataType( 8, 2, double.class, double[].class ) {
        public void readValues( Buf buf, long offset, int numElems,
                                Object array, int n ) {
            buf.readDataDoubles( offset, n * 2, (double[]) array );
        }
        public Object getScalar( Object array, int index ) {
            double[] darray = (double[]) array;
            return new double[] { darray[ index ], darray[ index + 1 ] };
        }
    };
    public static final DataType BYTE = INT1;
    public static final DataType FLOAT = REAL4;
    public static final DataType DOUBLE = REAL8;
    public static final DataType EPOCH = REAL8;
    public static final DataType TIME_TT2000 = INT8;
    public static final DataType UCHAR = CHAR;
    
    private static final DataType[] TYPE_TABLE = createTypeTable();

    private DataType( int byteCount, int groupSize,
                      Class<?> arrayElementClass, Class<?> scalarClass ) {
        byteCount_ = byteCount;
        groupSize_ = groupSize;
        arrayElementClass_ = arrayElementClass;
        scalarClass_ = scalarClass;
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
}
