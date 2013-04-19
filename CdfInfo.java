package cdf;

public class CdfInfo {
    private final NumericEncoding encoding_;
    private final boolean rowMajor_;
    private final int[] rDimSizes_;

    public CdfInfo( NumericEncoding encoding, boolean rowMajor,
                    int[] rDimSizes ) {
        encoding_ = encoding;
        rowMajor_ = rowMajor;
        rDimSizes_ = rDimSizes;
    }

    public NumericEncoding getEncoding() {
        return encoding_;
    }

    public boolean getRowMajor() {
        return rowMajor_;
    }

    public int[] getRDimSizes() {
        return rDimSizes_;
    }
}
