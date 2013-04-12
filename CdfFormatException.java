package cdf;

public class CdfFormatException extends RuntimeException {

    public CdfFormatException( String msg ) {
        super( msg );
    }

    public CdfFormatException( String msg, Throwable e ) {
        super( msg, e );
    }
}
