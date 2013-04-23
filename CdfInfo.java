package cdf;

public class CdfInfo {
    private final boolean rowMajor_;
    private final int[] rDimSizes_;

    public CdfInfo( boolean rowMajor, int[] rDimSizes ) {
        rowMajor_ = rowMajor;
        rDimSizes_ = rDimSizes;
    }

    public boolean getRowMajor() {
        return rowMajor_;
    }

    public int[] getRDimSizes() {
        return rDimSizes_;
    }
}
