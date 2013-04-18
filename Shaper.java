package cdf;

public abstract class Shaper {
    public Shaper( int[] dimSizes, boolean[] dimVarys, boolean rowMajor ) {
    }
    public abstract int getItemCount();
    public abstract Object shape( Object rawValue, boolean rowMajor );

    public static Shaper createShaper( int[] dimSizes, boolean[] dimVarys,
                                       boolean rowMajor ) {
  return null;
    }
}
