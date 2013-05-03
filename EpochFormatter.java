package cdf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Not thread-safe.
 */
public class EpochFormatter {

    private final DateFormat epochFormat_ =
        createDateFormat( "yyyy-MM-dd' 'HH:mm:ss.SSS" );
    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final long AD0_UNIX_MILLIS = getAd0UnixMillis();

    public String formatEpoch( double epoch ) {
        long unixMillis = (long) ( epoch + AD0_UNIX_MILLIS );
        Date date = new Date( unixMillis );
        return epochFormat_.format( date );
    }

    public String formatEpoch16( double epoch1, double epoch2 ) {
        return Double.toString( epoch1 ) + ", " + Double.toString( epoch2 );
    }

    public String formatTimeTt2000( long time ) {
        return Long.toString( time );
    }

    private static DateFormat createDateFormat( String pattern ) {
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( UTC );
        fmt.setCalendar( new GregorianCalendar( UTC, Locale.UK ) );
        return fmt;
    }

    private static long getAd0UnixMillis() {
        GregorianCalendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.setLenient( true );
        cal.clear();
        cal.set( 0, 0, 1, 0, 0, 0 );
        long ad0 = cal.getTimeInMillis();

        // Fudge factor to make this calculation match the apparent result
        // from the CDF library.  I don't understand why it's required.
        long fudge = 1000 * 60 * 60 * 24 * 2;  // 2 days
        return ad0 + fudge;
    }
}
