package uk.ac.bristol.star.cdf.test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.bristol.star.cdf.record.BankBuf;
import uk.ac.bristol.star.cdf.record.Buf;
import uk.ac.bristol.star.cdf.record.Pointer;
import uk.ac.bristol.star.cdf.record.SimpleNioBuf;

public class BufTest {

    private static boolean assertionsOn_;
    private final int blk_ = 54;
    private final int nn_ = 64;

    // Puts the various Buf implementations through their paces.
    public void testBufs() throws IOException {
        byte[] data = new byte[ 8 * 100 ];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream( bout );
        for ( int i = 0; i < nn_; i++ ) {
            dout.writeByte( -i );
            dout.writeByte( i );
            dout.writeShort( -i );
            dout.writeShort( i );
            dout.writeInt( -i );
            dout.writeInt( i );
            dout.writeLong( -i );
            dout.writeLong( i );
            dout.writeFloat( -i );
            dout.writeFloat( i );
            dout.writeDouble( -i );
            dout.writeDouble( i );
        }
        dout.flush();
        dout.close();
        byte[] bytes = bout.toByteArray();
        int nbyte = bytes.length;
        assert nbyte == blk_ * nn_;

        boolean isBit64 = false;
        boolean isBigEndian = true;
        ByteBuffer buf1 = ByteBuffer.wrap( bytes );
        checkBuf( new SimpleNioBuf( buf1, isBit64, isBigEndian ) );
        checkBuf( BankBuf.createSingleBankBuf( buf1, isBit64, isBigEndian ) );
        checkBuf( BankBuf.createMultiBankBuf( new ByteBuffer[] { buf1 },
                                              isBit64, isBigEndian ) );

        int[] banksizes =
            { 23, blk_ - 1, blk_ + 1, 49, blk_ * 4, blk_ * 2 + 2 };
        List<ByteBuffer> bblist = new ArrayList<ByteBuffer>();
        int ioff = 0;
        int ibuf = 0;
        int nleft = nbyte;
        while ( nleft > 0 ) {
            int leng = Math.min( banksizes[ ibuf % banksizes.length ], nleft );
            byte[] bb = new byte[ leng ];
            System.arraycopy( bytes, ioff, bb, 0, leng );
            bblist.add( ByteBuffer.wrap( bb ) );
            ibuf++;
            ioff += leng;
            nleft -= leng;
        }
        ByteBuffer[] bbufs = bblist.toArray( new ByteBuffer[ 0 ] );
        assert bbufs.length > 6;
        checkBuf( BankBuf
                 .createMultiBankBuf( bbufs, isBit64, isBigEndian ) );

        File tmpFile = File.createTempFile( "data", ".bin" );
        tmpFile.deleteOnExit();
        FileOutputStream fout = new FileOutputStream( tmpFile );
        fout.write( bytes );
        fout.close();
        FileChannel inchan = new FileInputStream( tmpFile ).getChannel();
        int[] banksizes2 = new int[ banksizes.length + 2 ];
        System.arraycopy( banksizes, 0, banksizes2, 0, banksizes.length );
        banksizes2[ banksizes.length + 0 ] = nbyte;
        banksizes2[ banksizes.length + 1 ] = nbyte * 2;
        for ( int banksize : banksizes2 ) {
            checkBuf( BankBuf.createMultiBankBuf( inchan, nbyte, banksize,
                                                  isBit64, isBigEndian ) );
        }
        tmpFile.delete();
    }

    private void checkBuf( Buf buf ) throws IOException {
        assert buf.getLength() == nn_ * blk_;
        byte[] abytes = new byte[ 2 ];
        short[] ashorts = new short[ 2 ];
        int[] aints = new int[ 4 ];
        long[] alongs = new long[ 2 ];
        float[] afloats = new float[ 21 ];
        double[] adoubles = new double[ 2 ];
        for ( int i = 0; i < nn_; i++ ) {
            int ioff = i * blk_;
            buf.readDataBytes( ioff + 0, 2, abytes );
            buf.readDataShorts( ioff + 2, 2, ashorts );
            buf.readDataInts( ioff + 6, 2, aints );
            buf.readDataLongs( ioff + 14, 2, alongs );
            buf.readDataFloats( ioff + 30, 2, afloats );
            buf.readDataDoubles( ioff + 38, 2, adoubles );
            assert abytes[ 0 ] == -i;
            assert abytes[ 1 ] == i;
            assert ashorts[ 0 ] == -i;
            assert ashorts[ 1 ] == i;
            assert aints[ 0 ] == -i;
            assert aints[ 1 ] == i;
            assert alongs[ 0 ] == -i;
            assert alongs[ 1 ] == i;
            assert afloats[ 0 ] == -i;
            assert afloats[ 1 ] == i;
            assert adoubles[ 0 ] == -i;
            assert adoubles[ 1 ] == i;
        }
        Pointer p = new Pointer( 0 );
        assert buf.readUnsignedByte( p ) == 0;
        assert buf.readUnsignedByte( p ) == 0;
        p.set( blk_ );
        assert buf.readUnsignedByte( p ) == 255;
        assert buf.readUnsignedByte( p ) == 1;
    }

    private static boolean checkAssertions() {
        assertionsOn_ = true;
        return true;
    }

    private static void runTests() throws IOException {
        assert checkAssertions();
        if ( ! assertionsOn_ ) {
            throw new RuntimeException( "Assertions disabled - bit pointless" );
        }
        BufTest test = new BufTest();
        test.testBufs();
    }

    public static void main( String[] args ) throws IOException {
        runTests();
    }
}
