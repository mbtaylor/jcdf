package cdf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public abstract class Compression {

    public static final Compression NONE =
        createFailCompression( "NONE", "Can't uncompress uncompressed data" );
    public static final Compression RLE =
        new RunLengthEncodingCompression( "RLE", (byte) 0 );
    public static final Compression HUFF =
        createUnsupportedCompression( "HUFF" );
    public static final Compression AHUFF =
        createUnsupportedCompression( "AHUFF" );
    public static final Compression GZIP = new GzipCompression( "GZIP" );

    // Get compression type code.  The mapping is missing from the
    // CDF Internal Format Description document, but cdf.h says:
    //    #define NO_COMPRESSION                  0L
    //    #define RLE_COMPRESSION                 1L
    //    #define HUFF_COMPRESSION                2L
    //    #define AHUFF_COMPRESSION               3L
    //    #define GZIP_COMPRESSION                5L
    private static final Compression[] KNOWN_COMPRESSIONS;
    static {
        KNOWN_COMPRESSIONS = new Compression[ 6 ];
        KNOWN_COMPRESSIONS[ 0 ] = NONE;
        KNOWN_COMPRESSIONS[ 1 ] = RLE;
        KNOWN_COMPRESSIONS[ 2 ] = HUFF;
        KNOWN_COMPRESSIONS[ 3 ] = AHUFF;
        KNOWN_COMPRESSIONS[ 5 ] = GZIP;
    };

    private final String name_;

    private Compression( String name ) {
        name_ = name;
    }

    /**
     * @param   outSize  the size of the uncompressed data
     */
    public abstract Buf uncompress( Buf inBuf, long inOffset, long outSize );

    public String getName() {
        return name_;
    }

    public static Compression getCompression( int cType ) {
        Compression[] kc = KNOWN_COMPRESSIONS;
        Compression comp = null;
        if ( cType >= 0 && cType < kc.length ) {
            comp = kc[ cType ];
        }
        return comp == null
             ? createFailCompression( "??",
                                      "Unknown compression format " + cType )
             : comp;
    }

    private static Compression createFailCompression( String name,
                                                      final String failMsg ) {
        return new Compression( name ) {
            public Buf uncompress( Buf inBuf, long inOffset, long outSize ) {
                throw new CdfFormatException( failMsg );
            }
        };
    }

    private static Compression createUnsupportedCompression( String name ) {
        return createFailCompression( name, "Unsupported compression format "
                                    + name );
    }

    private static abstract class FilterCompression extends Compression {
        FilterCompression( String name ) {
            super( name );
        }
        public Buf uncompress( Buf inBuf, long inOffset, long outSize ) {
            try {
                return attemptUncompress( inBuf, inOffset, outSize );
            }
            catch ( IOException e ) {
                throw new CdfFormatException( "Error decoding " + getName()
                                            + "-compressed data", e );
            }
        }
        private Buf attemptUncompress( Buf inBuf, long inOffset, long outSize )
                throws IOException {
            InputStream uin =
                uncompressStream(
                    new BufferedInputStream(
                        inBuf.createInputStream( inOffset ) ) );
            Buf ubuf = inBuf.fillNewBuf( outSize, uin );
            uin.close();
            return ubuf;
        }
        protected abstract InputStream uncompressStream( InputStream cin )
                throws IOException;
    }

    private static class RunLengthEncodingCompression
            extends FilterCompression {
        private final byte rleVal_;
        RunLengthEncodingCompression( String name, byte rleVal ) {
            super( name );
            rleVal_ = rleVal;
        }
        protected InputStream uncompressStream( InputStream in ) {
            return new RunLengthInputStream( in, rleVal_ );
        }
    }

    private static class GzipCompression extends FilterCompression {
        GzipCompression( String name ) {
            super( name );
        }
        protected InputStream uncompressStream( InputStream cin )
                throws IOException {
            return new GZIPInputStream( cin );
        }
    }
}
