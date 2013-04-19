package cdf;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class Shaper {

    public abstract int getRawItemCount();

    public abstract int getShapedItemCount();

    /**
     * The returned object is new; it is not rawValue.
     */
    public abstract Object shape( Object rawValue, boolean rowMajor );

    public abstract int getArrayIndex( int[] coords, boolean rowMajor );

    public static Shaper createShaper( DataType dataType,
                                       int[] dimSizes, boolean[] dimVarys,
                                       boolean rowMajor ) {
        int rawItemCount = 1;
        int shapedItemCount = 1;
        int nDimVary = 0;
        int ndim = dimSizes.length;
        for ( int idim = 0; idim < dimSizes.length; idim++ ) {
            int dimSize = dimSizes[ idim ];
            shapedItemCount *= dimSize;
            if ( dimVarys[ idim ] ) {
                nDimVary++;
                rawItemCount *= dimSize;
            }
        }
        if ( shapedItemCount == 1 ) {
            return new ScalarShaper( dataType );
        }
        else if ( ndim == 1  && nDimVary == 1 ) {
            assert shapedItemCount == rawItemCount;
            return new VectorShaper( dataType, shapedItemCount );
        }
        else if ( nDimVary == ndim ) {
            return new SimpleArrayShaper( dataType, dimSizes, rowMajor );
        }
        else {
            return new GeneralShaper( dataType, dimSizes, dimVarys, rowMajor );
        }
    }

    private static class ScalarShaper extends Shaper {
        private final DataType dataType_;
        ScalarShaper( DataType dataType ) {
            dataType_ = dataType;
        }
        public int getRawItemCount() {
            return 1;
        }
        public int getShapedItemCount() {
            return 1;
        }
        public Object shape( Object rawValue, boolean rowMajor ) {
            return dataType_.getScalar( rawValue, 0 );
        }
        public int getArrayIndex( int[] coords, boolean rowMajor ) {
            for ( int i = 0; i < coords.length; i++ ) {
                if ( coords[ i ] != 0 ) {
                    throw new IllegalArgumentException( "Out of bounds" );
                }
            }
            return 0;
        }
    }

    private static class VectorShaper extends Shaper {
        private final int itemCount_;
        private final int step_;
        VectorShaper( DataType dataType, int itemCount ) {
            itemCount_ = itemCount;
            step_ = dataType.getGroupSize();
        }
        public int getRawItemCount() {
            return itemCount_;
        }
        public int getShapedItemCount() {
            return itemCount_;
        }
        public Object shape( Object rawValue, boolean rowMajor ) {
            return rawValue;
        }
        public int getArrayIndex( int[] coords, boolean rowMajor ) {
            return coords[ 0 ] * step_;
        }
    }

    private static class GeneralShaper extends Shaper {

        private final DataType dataType_;
        private final int[] dimSizes_;
        private final boolean[] dimVarys_;
        private final boolean rowMajor_;
        private final int ndim_;
        private final int rawItemCount_;
        private final int shapedItemCount_;
        private final int[] strides_;
        private final int itemSize_;
        
        GeneralShaper( DataType dataType, int[] dimSizes, boolean[] dimVarys,
                       boolean rowMajor ) {
            dataType_ = dataType;
            dimSizes_ = dimSizes;
            dimVarys_ = dimVarys;
            rowMajor_ = rowMajor;
            ndim_ = dimSizes.length;

            int rawItemCount = 1;
            int shapedItemCount = 1;
            int nDimVary = 0;
            int ndim = dimSizes.length;
            strides_ = new int[ ndim_ ];
            for ( int idim = 0; idim < ndim_; idim++ ) {
                int jdim = rowMajor ? ndim_ - idim - 1 : idim;
                int dimSize = dimSizes[ jdim ];
                shapedItemCount *= dimSize;
                if ( dimVarys[ jdim ] ) {
                    nDimVary++;
                    rawItemCount *= dimSize;
                    strides_[ jdim ] = rawItemCount;
                }
            }
            rawItemCount_ = rawItemCount;
            shapedItemCount_ = shapedItemCount;
            itemSize_ = dataType_.getGroupSize();
        }

        public int getRawItemCount() {
            return rawItemCount_;
        }

        public int getShapedItemCount() {
            return shapedItemCount_;
        }

        public int getArrayIndex( int[] coords, boolean rowMajor ) {
            int index = 0;
            for ( int idim = 0; idim < ndim_; idim++ ) {
                int jdim = rowMajor ? ndim_ - idim - 1 : idim;
                index += coords[ idim ] * strides_[ jdim ];
            }
            return index * itemSize_;
        }

        public Object shape( Object rawValue, boolean rowMajor ) {
            Object out = Array.newInstance( dataType_.getArrayElementClass(),
                                            shapedItemCount_ * itemSize_ );
            int[] coords = new int[ ndim_ ];
            for ( int ix = 0; ix < shapedItemCount_; ix++ ) {
                for ( int idim = 0; idim < ndim_; idim++ ) {
                    int jdim = rowMajor ? ndim_ - idim - 1 : idim;
                    if ( ++coords[ jdim ] % dimSizes_[ jdim ] != 0 ) {
                        break;
                    }
                }
                System.arraycopy( rawValue, getArrayIndex( coords, rowMajor ),
                                  out, ix * itemSize_, itemSize_ );
            }
            return out;
        }
    }

    private static class SimpleArrayShaper extends GeneralShaper {

        private final DataType dataType_;
        private final boolean rowMajor_;

        public SimpleArrayShaper( DataType dataType, int[] dimSizes,
                                  boolean rowMajor ) {
            super( dataType, dimSizes, trueArray( dimSizes.length ),
                   rowMajor );
            dataType_ = dataType;
            rowMajor_ = rowMajor;
        }

        public Object shape( Object rawValue, boolean rowMajor ) {
            if ( rowMajor == rowMajor_ ) {
                int count = Array.getLength( rawValue );
                Object out =
                    Array.newInstance( dataType_.getArrayElementClass(),
                                       count );
                System.arraycopy( rawValue, 0, out, 0, count );
                return out;
            }
            else {
                // Probably there's a more efficient way to do this -
                // it's an n-dimensional generalisation of transposing
                // a matrix (though don't forget to keep units of
                // groupSize intact).
                return super.shape( rawValue, rowMajor );
            }
        }

        private static boolean[] trueArray( int n ) {
            boolean[] a = new boolean[ n ];
            Arrays.fill( a, true );
            return a;
        }
    }
}
