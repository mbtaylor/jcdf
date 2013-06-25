package uk.ac.bristol.star.cdf;

/**
 * Encapsulates some global information about a CDF file.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public class CdfInfo {
    private final boolean rowMajor_;
    private final int[] rDimSizes_;

    /**
     * Constructor.
     *
     * @param  rowMajor  true for row majority, false for column majority
     * @param  rDimSizes   array of dimension sizes for rVariables
     */
    public CdfInfo( boolean rowMajor, int[] rDimSizes ) {
        rowMajor_ = rowMajor;
        rDimSizes_ = rDimSizes;
    }

    /**
     * Indicates majority of CDF arrays.
     *
     * @return  true for row majority, false for column majority
     */
    public boolean getRowMajor() {
        return rowMajor_;
    }

    /**
     * Returns array dimensions for rVariables.
     *
     * @return  array of dimension sizes for rVariables
     */
    public int[] getRDimSizes() {
        return rDimSizes_;
    }
}
