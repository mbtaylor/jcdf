package cdf;

public enum NumericEncoding {

    NETWORK( Boolean.TRUE ),
    SUN( Boolean.TRUE ),
    NeXT( Boolean.TRUE ),
    MAC( Boolean.TRUE ),
    HP( Boolean.TRUE ),
    SGi( Boolean.TRUE ),
    IBMRS( Boolean.TRUE ),

    DECSTATION( Boolean.FALSE ),
    IBMPC( Boolean.FALSE ),
    ALPHAOSF1( Boolean.FALSE ),
    ALPHAVMSi( Boolean.FALSE ),

    VAX( null ),
    ALPHAVMSd( null ),
    ALPHAVMSg( null );

    private static final NumericEncoding[] encodings_ = createEncodingTable();

    private final Boolean isBigendian_;

    NumericEncoding( Boolean isBigendian ) {
        isBigendian_ = isBigendian;
    }

    public Boolean isBigendian() {
        return isBigendian_;
    }

    public static NumericEncoding getEncoding( int code )
            throws CdfFormatException {
        NumericEncoding encoding = code >= 0 && code < encodings_.length
                                 ? encodings_[ code ]
                                 : null;
        if ( encoding != null ) {
            return encoding;
        }
        else {
            throw new CdfFormatException( "Unknown encoding " + code );
        }
    }

    private static final NumericEncoding[] createEncodingTable() {
        NumericEncoding[] encodings = new NumericEncoding[ 17 ];
        encodings[ 1 ] = NETWORK;
        encodings[ 2 ] = SUN;
        encodings[ 3 ] = VAX;
        encodings[ 4 ] = DECSTATION;
        encodings[ 5 ] = SGi;
        encodings[ 6 ] = IBMPC;
        encodings[ 7 ] = IBMRS;
        encodings[ 9 ] = MAC;
        encodings[ 11 ] = HP;
        encodings[ 12 ] = NeXT;
        encodings[ 13 ] = ALPHAOSF1;
        encodings[ 14 ] = ALPHAVMSd;
        encodings[ 15 ] = ALPHAVMSg;
        encodings[ 16 ] = ALPHAVMSi;
        return encodings;
    }
}
