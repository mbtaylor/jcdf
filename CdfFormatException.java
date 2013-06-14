package cdf;

import java.io.IOException;

public class CdfFormatException extends IOException {

    public CdfFormatException( String msg ) {
        super( msg );
    }

    public CdfFormatException( String msg, Throwable e ) {
        super( msg );
        initCause( e );
    }
}
