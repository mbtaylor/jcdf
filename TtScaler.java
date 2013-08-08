package uk.ac.bristol.star.cdf.record;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles conversions between TT_TIME2000 (TT since J2000.0)
 * and Unix (UTC since 1970-01-01) times.
 * An instance of this class is valid for a certain range of TT2000 dates
 * (one that does not straddle a leap second).
 * To convert between TT_TIME2000 and Unix time, first acquire the
 * right instance of this class for the given time, and then use it
 * for the conversion.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2013
 */
public class TtScaler {

    private final double fixOffset_;
    private final double scaleBase_;
    private final double scaleFactor_;
    private final long fromTt2kMillis_;
    private final long toTt2kMillis_;

    /** Number of milliseconds in a day. */
    private static final double MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

    /** Date of the J2000 epoch (2000-01-01T12:00:00) as a Unix time. */
    private static final double J2000_UNIXMILLIS = 946728000000.0;

    /** Date of the J2000 epoch as a Modified Julian Date. */
    private static final double J2000_MJD = 51544.5;

    /** Date of the Unix epoch (1970-01-01T00:00:00) as an MJD. */
    private static final double UNIXEPOCH_MJD = 40587.0;

    /** TT is ahead of TAI by approximately 32.184 seconds. */
    private static final double TT_TAI_MILLIS = 32184;

    /**
     * TT2000 coefficients:
     *    year, month (1=Jan), day_of_month (1-based),
     *    fix_offset, scale_base, scale_factor.
     * year month day_of_month:
     * TAI-UTC= fix_offset S + (MJD - scale_base) * scale_factor S
     *
     * <p>Array initialiser lifted from gsfc.nssdc.cdf.util.CDFTT2000
     * source code.  That derives it from
     * http://maia.usno.navy.mil/ser7/tai-utc.dat.
     */
    private static final double[][] LTS = new double[][] {
        { 1960,  1,  1,  1.4178180, 37300.0, 0.0012960 },
        { 1961,  1,  1,  1.4228180, 37300.0, 0.0012960 },
        { 1961,  8,  1,  1.3728180, 37300.0, 0.0012960 },
        { 1962,  1,  1,  1.8458580, 37665.0, 0.0011232 },
        { 1963, 11,  1,  1.9458580, 37665.0, 0.0011232 },
        { 1964,  1,  1,  3.2401300, 38761.0, 0.0012960 },
        { 1964,  4,  1,  3.3401300, 38761.0, 0.0012960 },
        { 1964,  9,  1,  3.4401300, 38761.0, 0.0012960 },
        { 1965,  1,  1,  3.5401300, 38761.0, 0.0012960 },
        { 1965,  3,  1,  3.6401300, 38761.0, 0.0012960 },
        { 1965,  7,  1,  3.7401300, 38761.0, 0.0012960 },
        { 1965,  9,  1,  3.8401300, 38761.0, 0.0012960 },
        { 1966,  1,  1,  4.3131700, 39126.0, 0.0025920 },
        { 1968,  2,  1,  4.2131700, 39126.0, 0.0025920 },
        { 1972,  1,  1, 10.0,           0.0, 0.0       },
        { 1972,  7,  1, 11.0,           0.0, 0.0       },
        { 1973,  1,  1, 12.0,           0.0, 0.0       },
        { 1974,  1,  1, 13.0,           0.0, 0.0       },
        { 1975,  1,  1, 14.0,           0.0, 0.0       },
        { 1976,  1,  1, 15.0,           0.0, 0.0       },
        { 1977,  1,  1, 16.0,           0.0, 0.0       },
        { 1978,  1,  1, 17.0,           0.0, 0.0       },
        { 1979,  1,  1, 18.0,           0.0, 0.0       },
        { 1980,  1,  1, 19.0,           0.0, 0.0       },
        { 1981,  7,  1, 20.0,           0.0, 0.0       },
        { 1982,  7,  1, 21.0,           0.0, 0.0       },
        { 1983,  7,  1, 22.0,           0.0, 0.0       },
        { 1985,  7,  1, 23.0,           0.0, 0.0       },
        { 1988,  1,  1, 24.0,           0.0, 0.0       },
        { 1990,  1,  1, 25.0,           0.0, 0.0       },
        { 1991,  1,  1, 26.0,           0.0, 0.0       },
        { 1992,  7,  1, 27.0,           0.0, 0.0       },
        { 1993,  7,  1, 28.0,           0.0, 0.0       },
        { 1994,  7,  1, 29.0,           0.0, 0.0       },
        { 1996,  1,  1, 30.0,           0.0, 0.0       },
        { 1997,  7,  1, 31.0,           0.0, 0.0       },
        { 1999,  1,  1, 32.0,           0.0, 0.0       },
        { 2006,  1,  1, 33.0,           0.0, 0.0       },
        { 2009,  1,  1, 34.0,           0.0, 0.0       },
        { 2012,  7,  1, 35.0,           0.0, 0.0       }
    };
    private static TtScaler[] ORDERED_INSTANCES = createTtScalers();

    private static final Logger logger_ =
        Logger.getLogger( TtScaler.class.getName() );

    /**
     * Constructor.
     *
     * @param   fixOffset  fixed offset of UTC in seconds from TAI
     * @param   scaleBase  MJD base for scaling
     * @param   scaleFactor   factor for scaling
     * @param   fromTt2kMillis  start of validity range
     *                          in TT milliseconds since J2000
     * @param   toTt2kMillis    end of validity range
     *                          in TT milliseconds since J2000
     */
    public TtScaler( double fixOffset, double scaleBase, double scaleFactor,
                     long fromTt2kMillis, long toTt2kMillis ) {
        fixOffset_ = fixOffset;
        scaleBase_ = scaleBase;
        scaleFactor_ = scaleFactor;
        fromTt2kMillis_ = fromTt2kMillis;
        toTt2kMillis_ = toTt2kMillis;
    }

    /**
     * Converts time in milliseconds from TT since J2000 to UTC since 1970
     * for this scaler.
     *
     * @param  tt2kMillis  TT milliseconds since J2000
     * @return  UTC milliseconds since Unix epoch
     */
    public double tt2kToUnixMillis( long tt2kMillis ) {
        return tt2kToUnixMillis( tt2kMillis,
                                 fixOffset_, scaleBase_, scaleFactor_ );
    }

    /**
     * Assesses validity of this scaler for a given time.
     * The result will be zero if this scaler is valid,
     * negative if the given time is earlier than this scaler's range, and
     * positive if the given time is later than this scaler's range.
     *
     * @param  tt2kMillis  TT milliseconds since J2000
     * @return  validity signum
     */
    public int compareTt2kMillis( long tt2kMillis ) {
        if ( tt2kMillis < fromTt2kMillis_ ) {
            return -1;
        }
        else if ( tt2kMillis > toTt2kMillis_ ) {
            return +1;
        }
        else {
            return 0;
        }
    }

    /**
     * Searches an ordered array of scaler instances for one that is
     * applicable to a supplied TT time.
     * The supplied array of instances must be ordered and cover the
     * supplied time value; the result of {@link #getTtScalers} is suitable
     * and most likely what you want to use here.
     *
     * @param   tt2kMillis  TT time in milliseconds since J2000
     * @param   orderedScalers  list of TtScaler instances ordered in time
     * @param   i0  initial guess at index of the right answer;
     *              if negative no best guess is assumed
     */
    public static int getScalerIndex( long tt2kMillis,
                                      TtScaler[] orderedScalers, int i0 ) {
        int ns = orderedScalers.length;
        return scalerBinarySearch( tt2kMillis, orderedScalers,
                                   i0 >= 0 ? i0 : ns / 2, 0, ns - 1 );
    }

    /**
     * Recursive binary search of an ordered array of scaler instances
     * for one that covers a given point in time.
     *
     * @param   tt2kMillis  TT time in milliseconds since J2000
     * @param   orderedScalers  list of TtScaler instances ordered in time
     * @param   i0  initial guess at index of the right answer
     * @param   imin  minimum possible value of the right answer
     * @parma   imax  maximum possible value of the right answer
     */
    private static int scalerBinarySearch( long tt2kMillis, TtScaler[] scalers,
                                           int i0, int imin, int imax ) {

        // If the guess is correct, return it directly.
        int icmp = scalers[ i0 ].compareTt2kMillis( tt2kMillis );
        if ( icmp == 0 ) {
            return i0;
        }

        // Sanity check.  This condition shouldn't happen, but could do
        // for one of two reasons: a programming error in this code,
        // or an improperly ordered scalers array.
        if ( i0 < imin || i0 > imax ) {
            return -1;
        }
        assert i0 >= imin && i0 <= imax;

        // Bisect up or down and recurse.
        if ( icmp < 0 ) {
            return scalerBinarySearch( tt2kMillis, scalers,
                                       i0 - ( i0 - imin + 1 ) / 2,
                                       imin, i0 - 1 );
        }
        else {
            assert icmp > 0;
            return scalerBinarySearch( tt2kMillis, scalers,
                                       i0 + ( imax - i0 + 1 ) / 2,
                                       i0 + 1, imax );
        }
    }

    /**
     * Converts time in milliseconds from TT since J2000 to UTC since 1970
     * for given coefficients.
     *
     * @param   tt2kMillis  TT milliseconds since J2000
     * @param   fixOffset  fixed offset of UTC in seconds from TAI
     * @param   scaleBase  MJD base for scaling
     * @param   scaleFactor   factor for scaling
     * @return  UTC milliseconds since Unix epoch
     */
    private static double tt2kToUnixMillis( long tt2kMillis, double fixOffset,
                                            double scaleBase,
                                            double scaleFactor ) {
        double mjd = ((double) tt2kMillis) / MILLIS_PER_DAY + J2000_MJD;
        double utcOffsetSec = fixOffset + ( mjd - scaleBase ) * scaleFactor;
        double utcOffsetMillis = utcOffsetSec * 1000;
        return tt2kMillis - TT_TAI_MILLIS - utcOffsetMillis + J2000_UNIXMILLIS;
    }

    /**
     * Converts time in milliseconds from UTC since 1970 to TT since J2000
     * for given coefficients.
     *
     * @param   unixMillis  UTC milliseconds since the Unix epoch
     * @param   fixOffset  fixed offset of UTC in seconds from TAI
     * @param   scaleBase  MJD base for scaling
     * @param   scaleFactor   factor for scaling
     * @return  TT milliseconds since J2000
     */
    private static double unixToTt2kMillis( long unixMillis, double fixOffset,
                                            double scaleBase,
                                            double scaleFactor ) {
        double mjd = ((double) unixMillis) / MILLIS_PER_DAY + UNIXEPOCH_MJD;
        double utcOffsetSec = fixOffset + ( mjd - scaleBase ) * scaleFactor;
        double utcOffsetMillis = utcOffsetSec * 1000;
        return unixMillis + TT_TAI_MILLIS + utcOffsetMillis - J2000_UNIXMILLIS;
    }

    /**
     * Returns an ordered list of scalers covering the whole range of times.
     * Ordering is by time, as per the {@link #compareTt2kMillis} method;
     * every long <code>tt2kMillis</code> value will be valid for one of
     * the list.
     *
     * @return  ordered list of time scalers
     */
    public static TtScaler[] getTtScalers() {
        return ORDERED_INSTANCES.clone();
    }

    /**
     * Creates an ordered list of instances covering the whole range of times.
     *
     * @return  ordered list of time scaler instances
     */
    private static TtScaler[] createTtScalers() {
        int np = LTS.length;
        List<TtScaler> list = new ArrayList<TtScaler>();
        list.add( new TtScaler( 0, 0, 0, Long.MIN_VALUE,
                                ltCoeffsToTt2kMillis( LTS[ 0 ] ) ) );
        for ( int ip = 0; ip < np; ip++ ) {
            double[] coeffs = LTS[ ip ];
            double fixOffset = coeffs[ 3 ];
            double scaleBase = coeffs[ 4 ];
            double scaleFactor = coeffs[ 5 ];
            long fromValid = ltCoeffsToTt2kMillis( coeffs );
            long toValid = ip + 1 < np ? ltCoeffsToTt2kMillis( LTS[ ip + 1 ] )
                                       : Long.MAX_VALUE;
            list.add( new TtScaler( fixOffset, scaleBase, scaleFactor,
                                   fromValid, toValid ) );
        }
        return list.toArray( new TtScaler[ 0 ] );
    }

    /**
     * Returns the number of milliseconds in TT since J2000 corresponding
     * to one entry in the LTS array.
     *
     * @param  6-element LTS entry (year, month, dom, fixoff, base, factor)
     * @return   TT millis since J2000
     */
    private static long ltCoeffsToTt2kMillis( double[] ltCoeffs ) {
        int year = (int) ltCoeffs[ 0 ];
        int month = (int) ltCoeffs[ 1 ];
        int dom = (int) ltCoeffs[ 2 ];
        double fixOffset = ltCoeffs[ 3 ];
        double scaleBase = ltCoeffs[ 4 ];
        double scaleFactor = ltCoeffs[ 5 ];
        assert year == ltCoeffs[ 0 ];
        assert month == ltCoeffs[ 1 ];
        assert dom == ltCoeffs[ 2 ];
        long unixMillis = new GregorianCalendar( year, month - 1, dom )
                         .getTimeInMillis();
        return (long) TtScaler.unixToTt2kMillis( unixMillis, fixOffset,
                                                 scaleBase, scaleFactor );
    }
}
