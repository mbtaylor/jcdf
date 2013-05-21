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

    private static int read1( InputStream in ) throws IOException {
        int b = in.read();
        if ( b < 0 ) {
            throw new EOFException();
        }
        return b;
    }

    public static class HuffmanInputStream extends BitExpandInputStream {

        private static final int END_OF_STREAM = 256;
        private final Node[] nodes_;
        private final int iRoot_;
        private boolean ended_;

        public HuffmanInputStream( InputStream base ) throws IOException {
            super( base );
            nodes_ = inputCounts( base );
            iRoot_ = buildTree( nodes_ );
        }

        public int read() throws IOException {
            if ( ended_ ) {
                return -1;
            }
            int inode = iRoot_;
            do {
                Node node = nodes_[ inode ];
                boolean bit = readBit();
                inode = bit ? node.child1_ : node.child0_;
            } while ( inode > END_OF_STREAM );
          
            if ( inode == END_OF_STREAM ) {
                ended_ = true;
                return -1;
            }
            else {
                return inode;
            }
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
    }

    private static class Node {
        int count_;
        int savedCount_;
        int child0_;
        int child1_;
    }
}
