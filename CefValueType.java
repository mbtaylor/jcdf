package cef;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

abstract class CefValueType {

    private static final Map<String,CefValueType> vtMap = createValueTypeMap();
    private static final CefValueType STRING = new StringValueType( "string" );
    private static final Logger logger_ =
        Logger.getLogger( CefValueType.class.getName() );

    private final String name_;
    private final Class scalarClazz_;
    private final Class arrayClazz_;

    private CefValueType( String name, Class scalarClazz, Class arrayClazz ) {
        name_ = name;
        scalarClazz_ = scalarClazz;
        arrayClazz_ = arrayClazz;
    }

    public Class getScalarClass() {
        return scalarClazz_;
    }

    public Class getArrayClass() {
        return arrayClazz_;
    }

    public abstract Object parseScalarValue( String entry );
    public abstract Object parseArrayValues( String[] entries,
                                             int start, int count );

    /**
     * Returns a 1-element array containing an array value suitable for
     * use as a blank value.  The <code>blankEntry</code> value supplied
     * is the string representation of this blank value.  It should be
     * ignored if possible, but it may be used as a hint for the 
     * blank value if required.
     */
    public abstract Object createBlankUnitArray( String blankEntry );

    void warnFail( String txt ) {
        logger_.warning( "Failed to parse " + name_ + " value "
                       + "\"" + txt + "\"" );
    }

    /** does not return null. */
    public static CefValueType getValueType( String vt ) {
        CefValueType type = vtMap.get( vt );
        return type == null ? STRING : type;
    }

    private static Map<String,CefValueType> createValueTypeMap() {
        Map<String,CefValueType> map = new HashMap<String,CefValueType>();
        map.put( "FLOAT", new FloatValueType( "FLOAT" ) );
        map.put( "DOUBLE", new DoubleValueType( "DOUBLE" ) );
        map.put( "INT", new IntegerValueType( "INT" ) );
        map.put( "BYTE", new ByteValueType( "BYTE" ) );
        map.put( "ISO_TIME", new StringValueType( "ISO_TIME" ) );
        return map;
    }

    private static class FloatValueType extends CefValueType {
        FloatValueType( String name ) {
            super( name, Float.class, float[].class );
        }
        public Object parseScalarValue( String item ) {
            try {
                return Float.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public Object parseArrayValues( String[] items, int start, int count ) {
            float[] results = new float[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                float value;
                try {
                    value = Float.parseFloat( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = Float.NaN;
                }
                results[ i ] = value;
            }
            return results;
        }
        public Object createBlankUnitArray( String blankEntry ) {
            return new float[] { Float.NaN };
        }
    }

    private static class DoubleValueType extends CefValueType {
        DoubleValueType( String name ) {
            super( name, Double.class, double[].class );
        }
        public Object parseScalarValue( String item ) {
            try {
                return Double.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public Object parseArrayValues( String[] items, int start, int count ) {
            double[] results = new double[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                double value;
                try {
                    value = Double.parseDouble( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = Double.NaN;
                }
                results[ i ] = value;
            }
            return results;
        }
        public Object createBlankUnitArray( String blankEntry ) {
            return new double[] { Double.NaN };
        }
    }

    private static class IntegerValueType extends CefValueType {
        IntegerValueType( String name ) {
            super( name, Integer.class, int[].class );
        }
        public Object parseScalarValue( String item ) {
            try {
                return Integer.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public Object parseArrayValues( String[] items, int start, int count ) {
            int[] results = new int[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                int value;
                try {
                    value = Integer.parseInt( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = 0;
                }
                results[ i ] = value;
            }
            return results;
        }
        public Object createBlankUnitArray( String blankEntry ) {
            int blankVal = 0;
            try {
                blankVal = Integer.parseInt( blankEntry );
            }
            catch ( NumberFormatException e ) {
                // too bad
                blankVal = 0;
            }
            return new int[] { blankVal };
        }
    }

    // CEF specification does not say whether byte is signed.  Assume so.
    private static class ByteValueType extends CefValueType {
        ByteValueType( String name ) {
            super( name, Byte.class, byte[].class );
        }
        public Object parseScalarValue( String item ) {
            try {
                return Byte.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public Object parseArrayValues( String[] items, int start, int count ) {
            byte[] results = new byte[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                int ivalue;
                try {
                    ivalue = Integer.parseInt( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    ivalue = 0;
                }
                byte bvalue = (byte) ivalue;
                if ( bvalue != ivalue ) {
                    logger_.warning( "truncated byte value "
                                    + ivalue + " -> " + bvalue );
                }
                results[ i ] = bvalue;
            }
            return results;
        }
        public Object createBlankUnitArray( String blankEntry ) {
            byte blankVal = 0;
            try {
                blankVal = Byte.parseByte( blankEntry );
            }
            catch ( NumberFormatException e ) {
                // too bad
                blankVal = (byte) 0;
            }
            return new byte[] { blankVal };
        }
    }

    private static class StringValueType extends CefValueType {
        StringValueType( String name ) {
            super( name, String.class, String[].class );
        }
        public Object parseScalarValue( String item ) {
            return item;
        }
        public Object parseArrayValues( String[] items, int start, int count ) {
            String[] results = new String[ count ];
            System.arraycopy( items, start, results, 0, count );
            return results;
        }
        public Object createBlankUnitArray( String blankEntry ) {
            return new String[ 1 ];
        }
    }
}
