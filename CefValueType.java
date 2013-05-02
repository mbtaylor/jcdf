package cef;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class CefValueType {

    private static final Map<String,CefValueType> vtMap = createValueTypeMap();
    private static final CefValueType STRING = new StringValueType( "string" );
    private static final Logger logger_ =
        Logger.getLogger( CefValueType.class.getName() );
    private static final Level substLevel_ = Level.WARNING;

    private final String name_;
    private final Class scalarClazz_;
    private final Class arrayClazz_;

    private CefValueType( String name, Class scalarClazz, Class arrayClazz ) {
        name_ = name;
        scalarClazz_ = scalarClazz;
        arrayClazz_ = arrayClazz;
    }

    public String getName() {
        return name_;
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
     * Substitute a blank value for every occurrence of a given magic
     * value in an array, if possible.
     *
     * @param  array   array whose elements are to be blanked
     * @param  magic1  1-element array containing the magic value
     */
    public abstract void substituteBlanks( Object array, Object magic1 );

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
        public void substituteBlanks( Object array, Object magic1 ) {
            float magic = ((float[]) magic1)[ 0 ];
            float[] farray = (float[]) array;
            int count = farray.length;
            for ( int i = 0; i < count; i++ ) {
                if ( farray[ i ] == magic ) {
                    farray[ i ] = Float.NaN;
                }
            }
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
        public void substituteBlanks( Object array, Object magic1 ) {
            double magic = ((double[]) magic1)[ 0 ];
            double[] darray = (double[]) array;
            int count = darray.length;
            for ( int i = 0; i < count; i++ ) {
                if ( darray[ i ] == magic ) {
                    darray[ i ] = Double.NaN;
                }
            }
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
        public void substituteBlanks( Object array, Object magic1 ) {
            if ( logger_.isLoggable( substLevel_ ) ) {
                int magic = ((int[]) magic1)[ 0 ];
                int[] iarray = (int[]) array;
                int count = iarray.length;
                int nsub = 0;
                for ( int i = 0; i < count; i++ ) {
                    if ( iarray[ i ] == magic ) {
                        nsub++;
                    }
                }
                if ( nsub > 0 ) {
                    logger_.log( substLevel_,
                                 nsub + " fill values not substitued in "
                               + getName() + " column" );
                }
            }
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
            int ntrunc = 0;
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
                    ntrunc++;
                    logger_.info( "truncated byte value "
                                 + ivalue + " -> " + bvalue );
                }
                results[ i ] = bvalue;
            }
            if ( ntrunc > 0 ) {
                logger_.warning( "Truncated " + ntrunc + " values in "
                               + getName() + " column" );
            }
            return results;
        }
        public void substituteBlanks( Object array, Object magic1 ) {
            if ( logger_.isLoggable( substLevel_ ) ) {
                byte magic = ((byte[]) magic1)[ 0 ];
                byte[] barray = (byte[]) array;
                int count = barray.length;
                int nsub = 0;
                for ( int i = 0; i < count; i++ ) {
                    if ( barray[ i ] == magic ) {
                        nsub++;
                    }
                }
                if ( nsub > 0 ) {
                    logger_.log( substLevel_,
                                 nsub + " fill values not substituted in "
                               + getName() + " column" );
                }
            }
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
        public void substituteBlanks( Object array, Object magic1 ) {
            String magic = ((String[]) magic1)[ 0 ];
            String[] sarray = (String[]) array;
            int count = sarray.length;
            for ( int i = 0; i < count; i++ ) {
                if ( sarray[ i ].equals( magic ) ) {
                    sarray[ i ] = null;
                }
            }
        }
    }
}
