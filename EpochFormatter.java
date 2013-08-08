package uk.ac.bristol.star.cdf.record;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Does string formatting of epoch values in various representations.
 * The methods of this object are not in general thread-safe.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2013
 */
public class EpochFormatter {

    private final DateFormat epochMilliFormat_ =
        createDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" );
    private final DateFormat epochSecFormat_ =
        createDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
    private int iLastTtScaler_ = -1;

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private static final long AD0_UNIX_MILLIS = getAd0UnixMillis();
    private static final TtScaler[] TT_SCALERS = TtScaler.getTtScalers();

    /**
     * Formats a CDF EPOCH value as an ISO-8601 date.
     *
     * @param  epoch  EPOCH value
     * @return   date string
     */
    public String formatEpoch( double epoch ) {
        long unixMillis = (long) ( epoch + AD0_UNIX_MILLIS );
        Date date = new Date( unixMillis );
        return epochMilliFormat_.format( date );
    }

    /**
     * Formats a CDF EPOCH16 value as an ISO-8601 date.
     *
     * @param   epoch1  first element of EPOCH16 pair (seconds since 0AD)
     * @param   epoch2  second element of EPOCH16 pair (additional picoseconds)
     * @return  date string
     */
    public String formatEpoch16( double epoch1, double epoch2 ) {
        long unixMillis = (long) ( epoch1 * 1000 ) + AD0_UNIX_MILLIS;
        Date date = new Date( unixMillis );
        long plusPicos = (long) epoch2;
        if ( plusPicos < 0 || plusPicos >= 1e12 ) {
            return "??";
        }
        String result = new StringBuffer( 32 )
            .append( epochSecFormat_.format( date ) )
            .append( '.' )
            .append( prePadWithZeros( plusPicos, 12 ) )
            .toString();
        assert result.length() == 32;
        return result;
    }

    /**
     * Formats a CDF TIME_TT2000 value as an ISO-8601 date.
     *
     * @param  timeTt2k  TIME_TT2000 value
     * @return  date string
     */
    public String formatTimeTt2000( long timeTt2k ) {

        // Special case - see "Variable Pad Values" section
        // (sec 2.3.20 at v3.4, and footnote) of CDF Users Guide.
        if ( timeTt2k == Long.MIN_VALUE + 1 ) {
            return "0000-01-01T00:00:00.000000000";
        }

        long j2kMillis = timeTt2k / 1000000;
        int plusNanos = (int) ( timeTt2k % 1000000 );
        if ( plusNanos < 0 ) {
            j2kMillis--;
            plusNanos += 1000000;
        }
        double unixMillis = getTtScaler( j2kMillis )
                           .tt2kToUnixMillis( j2kMillis );
        Date date = new Date( (long) unixMillis );
        String txt = epochMilliFormat_.format( date );
        return txt + prePadWithZeros( plusNanos, 6 );
    }

    /**
     * Returns the TtScaler instance that is valid for a given time.
     *
     * @param  tt2kMillis  TT time since J2000 in milliseconds
     * @return  scaler
     */
    private TtScaler getTtScaler( long tt2kMillis ) {

        // Use the most recently used value as the best guess.
        // There's a good chance it's the right one.
        int index = TtScaler
                   .getScalerIndex( tt2kMillis, TT_SCALERS, iLastTtScaler_ );
        iLastTtScaler_ = index;
        return TT_SCALERS[ index ];
    }

    /**
     * Constructs a DateFormat object for a given pattern for UTC.
     *
     * @param  pattern  formatting pattern
     * @return  format
     * @see   java.text.SimpleDateFormat
     */
    private static DateFormat createDateFormat( String pattern ) {
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( UTC );
        fmt.setCalendar( new GregorianCalendar( UTC, Locale.UK ) );
        return fmt;
    }

    /**
     * Returns the CDF epoch (0000-01-01T00:00:00)
     * in milliseconds since the Unix epoch (1970-01-01T00:00:00).
     *
     * @return  -62,167,219,200,000
     */
    private static long getAd0UnixMillis() {
        GregorianCalendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.setLenient( true );
        cal.clear();
        cal.set( 0, 0, 1, 0, 0, 0 );
        long ad0 = cal.getTimeInMillis();

        // Fudge factor to make this calculation match the apparent result
        // from the CDF library.  Not quite sure why it's required, but
        // I think something to do with the fact that the first day is day 1
        // and signs around AD0/BC0.
        long fudge = 1000 * 60 * 60 * 24 * 2;  // 2 days
        return ad0 + fudge;
    }

    /**
     * Pads a numeric value with zeros to return a fixed length string
     * representing a given numeric value.
     *
     * @param  value  number
     * @param  leng   number of characters in result
     * @return   leng-character string containing value
     *           padded at start with zeros
     */
    private static String prePadWithZeros( long value, int leng ) {
        String txt = Long.toString( value );
        int nz = leng - txt.length();
        if ( nz == 0 ) {
            return txt;
        }
        else if ( nz < 0 ) {
            throw new IllegalArgumentException();
        }
        else {
            StringBuffer sbuf = new StringBuffer( leng );
            for ( int i = 0; i < nz; i++ ) {
                sbuf.append( '0' );
            }
            sbuf.append( txt );
            return sbuf.toString();
        }
    }
}
