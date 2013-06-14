package cdf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public abstract class Compression {

    public static final Compression NONE =
        createFailCompression( "NONE", "Can't uncompress uncompressed data" );
    public static final Compression RLE = new Compression( "RLE" ) {
        public InputStream uncompressStream( InputStream in )
                throws IOException {
            return new RunLengthInputStream( in, (byte) 0 );
        }
    };
    public static final Compression HUFF = new Compression( "HUFF" ) {
        public InputStream uncompressStream( InputStream in )
                throws IOException {
            return new BitExpandInputStream.HuffmanInputStream( in );
        }
    };
    public static final Compression AHUFF = new Compression( "AHUFF" ) {
        public InputStream uncompressStream( InputStream in )
                throws IOException {
            return new BitExpandInputStream.AdaptiveHuffmanInputStream( in );
        }
    };
    public static final Compression GZIP = new Compression( "GZIP" ) {
        public InputStream uncompressStream( InputStream in )
                throws IOException {
            return new GZIPInputStream( in );
        }
    };

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

    protected Compression( String name ) {
        name_ = name;
    }

    public abstract InputStream uncompressStream( InputStream in )
            throws IOException;

    public String getName() {
        return name_;
    }

    /**
     * Utility method.
     * @param   outSize  the size of the uncompressed data
     */
    public static Buf uncompress( Compression compression, Buf inBuf,
                                  long inOffset, long outSize )
            throws IOException {
        InputStream uin =
             compression
            .uncompressStream( new BufferedInputStream(
                                   inBuf.createInputStream( inOffset ) ) );
        Buf ubuf = inBuf.fillNewBuf( outSize, uin );
        uin.close();
        return ubuf;
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
            public InputStream uncompressStream( InputStream cin )
                    throws CdfFormatException {
                throw new CdfFormatException( failMsg );
            }
        };
    }

    private static Compression createUnsupportedCompression( String name ) {
        return createFailCompression( name, "Unsupported compression format "
                                    + name );
    }
}
