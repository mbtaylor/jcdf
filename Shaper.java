package cdf;

public abstract class Shaper {

    /**
     * grpSize is the number of array (primitive or String) elements
     * per shaped value element.
     * It's like numElems, but usually not the same value (since numElems
     * is taken care of by grouping bytes into strings elsewhere).
     * It has the value 1, except for EPOCH16 data type, which is represented
     * by a pair of doubles.  
     */
    public Shaper( int[] dimSizes, boolean[] dimVarys, boolean grpSize,
                   boolean rowMajor ) {
    }
    public abstract int getRawItemCount();
    public abstract int getShapedItemCount();
    public abstract Object shape( Object rawValue, boolean rowMajor );
    public abstract Object getItem( Object rawValue, boolean rowMajor,
                                    int[] coords );
    public static Shaper createShaper( int[] dimSizes, boolean[] dimVarys,
                                       boolean rowMajor ) {
  return null;
    }
}
