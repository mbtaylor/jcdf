package cdf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This code is based on the C implementation in "The Data Compression Book"
 * (Mark Nelson, 1992), via the code in cdfhuff.c of the CDF source
 * distribution.  Only decompression, not compression, is present/required.
 */
abstract class BitExpandInputStream extends InputStream {

    private final InputStream base_;
    private int rack_;
    private int mask_;
    private boolean ended_;

    private static final int END_OF_STREAM = 256;

    public BitExpandInputStream( InputStream base ) {
        base_ = base;
        mask_ = 0x80;
    }

    public void close() throws IOException {
        base_.close();
    }

    public boolean markSupported() {
        return false;
    }

    public int read() throws IOException {
        if ( ended_ ) {
            return -1;
        }
        int token = readToken();
        if ( token == END_OF_STREAM ) {
            ended_ = true;
            return -1;
        }
        else {
            return token;
        }
    }

    /**
     * Reads a single character.  The result may be either a byte value
     * in the range 0--255, or the terminator value END_OF_STREAM.
     * The actual end of the input stream should not be encountered
     * (it should be flagged by an END_OF_STREAM indicator token);
     * if it is, an EOFException is thrown.
     */
    protected abstract int readToken() throws IOException;

    public boolean readBit() throws IOException {
        if ( mask_ == 0x80 ) {
            rack_ = read1( base_ );
        }
        int value = rack_ & mask_;
        mask_ >>= 1;
        if ( mask_ == 0 ) {
            mask_ = 0x80;
        }
        return value != 0;
    }

    public int readBits( int bitCount ) throws IOException {
        int mask = 1 << ( bitCount - 1 );
        int value = 0;
        while ( mask != 0 ) {
            if ( readBit() ) {
                value |= mask;
            }
            mask >>= 1;
        }
        return value;
    }

    private static int read1( InputStream in ) throws IOException {
        int b = in.read();
        if ( b < 0 ) {
            throw new EOFException();
        }
        return b;
    }

    public static class HuffmanInputStream extends BitExpandInputStream {

        private final Node[] nodes_;
        private final int iRoot_;
        private boolean ended_;

        public HuffmanInputStream( InputStream base ) throws IOException {
            super( base );
            nodes_ = inputCounts( base );
            iRoot_ = buildTree( nodes_ );
        }

        protected int readToken() throws IOException {
            int inode = iRoot_;
            do {
                Node node = nodes_[ inode ];
                boolean bit = readBit();
                inode = bit ? node.child1_ : node.child0_;
            } while ( inode > END_OF_STREAM );
            return inode;
        }

        private static Node[] inputCounts( InputStream in ) throws IOException {
            Node[] nodes = new Node[ 514 ];
            for ( int i = 0; i < 514; i++ ) {
                nodes[ i ] = new Node();
            }
            int ifirst = read1( in );
            int ilast = read1( in );
            while ( true ) {
                for ( int i = ifirst; i <= ilast; i++ ) {
                    nodes[ i ].count_ = read1( in );
                }
                ifirst = read1( in );
                if ( ifirst == 0 ) {
                    break; 
                }
                ilast = read1( in );
            }
            nodes[ END_OF_STREAM ].count_ = 1;
            return nodes;
        }

        private static int buildTree( Node[] nodes ) {
            int min1;
            int min2;
            nodes[ 513 ].count_ = Integer.MAX_VALUE;
            int nextFree = END_OF_STREAM + 1;
            while ( true ) {
                min1 = 513;
                min2 = 513;
                for ( int i = 0; i < nextFree; i++ ) {
                    if ( nodes[ i ].count_ != 0 ) {
                        if ( nodes[ i ].count_ < nodes[ min1 ].count_ ) {
                            min2 = min1;
                            min1 = i;
                        }
                        else if ( nodes[ i ].count_ < nodes[ min2 ].count_ ) {
                            min2 = i;
                        }
                    }
                }
                if ( min2 == 513 ) {
                    break;
                }
                nodes[ nextFree ].count_ = nodes[ min1 ].count_
                                         + nodes[ min2 ].count_;
                nodes[ min1 ].savedCount_ = nodes[ min1 ].count_;
                nodes[ min1 ].count_ = 0;
                nodes[ min2 ].savedCount_ = nodes[ min2 ].count_;
                nodes[ min2 ].count_ = 0;
                nodes[ nextFree ].child0_ = min1;
                nodes[ nextFree ].child1_ = min2;
                nextFree++;
            }
            nextFree--;
            nodes[ nextFree ].savedCount_ = nodes[ nextFree ].count_;
            return nextFree;
        }

        private static class Node {
            int count_;
            int savedCount_;
            int child0_;
            int child1_;
        }
    }

    public static class AdaptiveHuffmanInputStream
            extends BitExpandInputStream {

        /* Tree members.  This class acts as its own tree. */
        private final int[] leafs_;
        private final Node[] nodes_;
        private int nextFreeNode_;

        private static final int ESCAPE = 257;
        private static final int SYMBOL_COUNT = 258;
        private static final int NODE_TABLE_COUNT = ( SYMBOL_COUNT * 2 ) - 1;
        private static final int ROOT_NODE = 0;
        private static final int MAX_WEIGHT = 0x8000;

        public AdaptiveHuffmanInputStream( InputStream base ) {
            super( base );

            // Initialise the tree.
            leafs_ = new int[ SYMBOL_COUNT ];
            nodes_ = new Node[ NODE_TABLE_COUNT ];
            nodes_[ ROOT_NODE ] = new Node( ROOT_NODE + 1, false, 2, -1 );
            nodes_[ ROOT_NODE + 1 ] = new Node( END_OF_STREAM, true, 1,
                                                ROOT_NODE );
            leafs_[ END_OF_STREAM ] = ROOT_NODE + 1;
            nodes_[ ROOT_NODE + 2 ] = new Node( ESCAPE, true, 1, ROOT_NODE );
            leafs_[ ESCAPE ] = ROOT_NODE + 2;
            nextFreeNode_ = ROOT_NODE + 3;
            for ( int i = 0; i < END_OF_STREAM; i++ ) {
                leafs_[ i ] = -1;
            }
        }

        protected int readToken() throws IOException {
            int iCurrentNode = ROOT_NODE;
            while ( ! nodes_[ iCurrentNode ].childIsLeaf_ ) {
                iCurrentNode = nodes_[ iCurrentNode ].child_;
                boolean bit = readBit();
                iCurrentNode += bit ? 1 : 0;
            }
            int c = nodes_[ iCurrentNode ].child_;
            if ( c == ESCAPE ) {
                c = readBits( 8 );
                addNewNode( c );
            }
            updateModel( c );
            return c;
        }

        private void addNewNode( int c ) {
            int iLightestNode = nextFreeNode_ - 1;
            int iNewNode = nextFreeNode_;
            int iZeroWeightNode = nextFreeNode_ + 1;
            nextFreeNode_ += 2;
            nodes_[ iNewNode ] = new Node( nodes_[ iLightestNode ] );
            nodes_[ iNewNode ].parent_ = iLightestNode;
            leafs_[ nodes_[ iNewNode ].child_ ] = iNewNode;
            nodes_[ iLightestNode ] =
                new Node( iNewNode, false, nodes_[ iLightestNode ].weight_,
                                           nodes_[ iLightestNode ].parent_ );
            nodes_[ iZeroWeightNode ] = new Node( c, true, 0, iLightestNode );
            leafs_[ c ] = iZeroWeightNode;
        }

        private void updateModel( int c ) {
            if ( nodes_[ ROOT_NODE ].weight_ == MAX_WEIGHT ) {
                rebuildTree();
            }
            int iCurrentNode = leafs_[ c ];
            while ( iCurrentNode != -1 ) {
                nodes_[ iCurrentNode ].weight_++;
                int iNewNode;
                for ( iNewNode = iCurrentNode; iNewNode > ROOT_NODE;
                      iNewNode-- ) {
                    if ( nodes_[ iNewNode - 1 ].weight_ >=
                         nodes_[ iCurrentNode ].weight_ ) {
                        break;
                    }
                }
                if ( iCurrentNode != iNewNode ) {
                    swapNodes( iCurrentNode, iNewNode );
                    iCurrentNode = iNewNode;
                }
                iCurrentNode = nodes_[ iCurrentNode ].parent_;
            }
        }

        private void swapNodes( int i, int j ) {
            if ( nodes_[ i ].childIsLeaf_ ) {
                leafs_[ nodes_[ i ].child_ ] = j;
            }
            else {
                nodes_[ nodes_[ i ].child_ ].parent_ = j;
                nodes_[ nodes_[ i ].child_ + 1 ].parent_ = j;
            }
            if ( nodes_[ j ].childIsLeaf_ ) {
                leafs_[ nodes_[ j ].child_ ] = i;
            }
            else {
                nodes_[ nodes_[ j ].child_ ].parent_ = i;
                nodes_[ nodes_[ j ].child_ + 1 ].parent_ = i;
            }
            Node temp = new Node( nodes_[ i ] );
            nodes_[ i ] = new Node( nodes_[ j ] );
            nodes_[ i ].parent_ = temp.parent_;
            temp.parent_ = nodes_[ j ].parent_;
            nodes_[ j ] = temp;
        }

        private void rebuildTree() {
            int j = nextFreeNode_ - 1;
            for ( int i = j; i >= ROOT_NODE; i-- ) {
                if ( nodes_[ i ].childIsLeaf_ ) {
                    nodes_[ j ] = new Node( nodes_[ i ] );
                    nodes_[ j ].weight_ = ( nodes_[ j ].weight_ + 1 ) / 2;
                    j--;
                }
            }

            for ( int i = nextFreeNode_ - 2; j >= ROOT_NODE; i -= 2, j-- ) {
                int k = i + 1;
                nodes_[ j ].weight_ = nodes_[ i ].weight_ + nodes_[ k ].weight_;
                int weight = nodes_[ j ].weight_;
                nodes_[ j ].childIsLeaf_ = false;
                for ( k = j + 1; weight < nodes_[ k ].weight_; k++ ) {
                }
                k--;
                System.arraycopy( nodes_, j + 1, nodes_, j, k - j );
                nodes_[ k ] = new Node( i, false, weight, nodes_[ k ].parent_ );
            }

            for ( int i = nextFreeNode_ - 1; i >= ROOT_NODE; i-- ) {
                if ( nodes_[ i ].childIsLeaf_ ) {
                    int k = nodes_[ i ].child_;
                    leafs_[ k ] = i;
                }
                else {
                    int k = nodes_[ i ].child_;
                    nodes_[ k ].parent_ = nodes_[ k + 1 ].parent_ = i;
                }
            }
        }

        private static class Node {
            int child_;
            boolean childIsLeaf_;
            int weight_;
            int parent_;
            Node( int child, boolean childIsLeaf, int weight, int parent ) {
                child_ = child;
                childIsLeaf_ = childIsLeaf;
                weight_ = weight;
                parent_ = parent;
            }
            Node( Node node ) {
                this( node.child_, node.childIsLeaf_, node.weight_,
                      node.parent_ );
            }
        }
    }
}
