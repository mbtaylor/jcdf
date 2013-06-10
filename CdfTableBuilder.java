package cdf.util;

import cdf.Buf;
import cdf.Bufs;
import cdf.CdfContent;
import cdf.CdfReader;
import cdf.WrapperBuf;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;

public class CdfTableBuilder implements TableBuilder {

    public static final CdfTableProfile DEFAULT_PROFILE = createProfile(
        true,
        new String[] { "FIELDNAM", "DESCRIP", "DESCRIPTION", },
        new String[] { "UNITS", "UNIT", "UNITSTRING", }
    );

    private final CdfTableProfile profile_;

    public CdfTableBuilder() {
        this( DEFAULT_PROFILE );
    }

    public CdfTableBuilder( CdfTableProfile profile ) {
        profile_ = profile;
    }

    public String getFormatName() {
        return "CDF";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( ! CdfReader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not a CDF file" );
        }
        final Buf nbuf;
        if ( datsrc instanceof FileDataSource &&
             datsrc.getCompression() == Compression.NONE ) {
            File file = ((FileDataSource) datsrc).getFile();
            nbuf = Bufs.createBuf( file, true, true );
        }
        else {
            ByteStore byteStore = storagePolicy.makeByteStore();
            BufferedOutputStream storeOut =
                new BufferedOutputStream( byteStore.getOutputStream() );
            InputStream dataIn = datsrc.getInputStream();
            IOUtils.copy( dataIn, storeOut );
            dataIn.close();
            storeOut.flush();
            ByteBuffer[] bbufs = byteStore.toByteBuffers();
            storeOut.close();
            byteStore.close();
            nbuf = Bufs.createBuf( bbufs, true, true );
        }

        // Fix the Buf implementation so that it uses the supplied
        // storage policy for allocating any more required storage.
        Buf buf = new StoragePolicyBuf( nbuf, storagePolicy );
        CdfContent content = new CdfReader( buf ).readCdf();
        return new CdfStarTable( content, profile_ );
    }

    /**
     * Returns false.  I don't think there is a MIME type associated with
     * the CDF format.  References to application/x-cdf and application/cdf
     * appear on the web, but neither is IANA registered, and I <em>think</em>
     * they refer to some other format.
     */
    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Throws a TableFormatException.
     * CDF is not suitable for streaming.
     */
    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new TableFormatException( "Can't stream from CDF format" );
    }

    private static class StoragePolicyBuf extends WrapperBuf {
        private final Buf baseBuf_;
        private final StoragePolicy storagePolicy_;

        StoragePolicyBuf( Buf baseBuf, StoragePolicy storagePolicy ) {
            super( baseBuf );
            baseBuf_ = baseBuf;
            storagePolicy_ = storagePolicy;
        }

        public Buf fillNewBuf( long count, InputStream in ) throws IOException {
            ByteStore byteStore = storagePolicy_.makeByteStore();
            OutputStream out = byteStore.getOutputStream();
            int bufsiz = 16384;
            byte[] a = new byte[ bufsiz ];
            for ( int ntot = 0; ntot < count; ) {
                int n = in.read( a, 0,
                                 Math.min( bufsiz, (int) ( count - ntot ) ) );
                if ( n < 0 ) {
                    throw new IOException( "Stream ended after " + ntot + "/"
                                         + count + " bytes" );
                }
                else {
                    out.write( a, 0, n );
                    ntot += n;
                }
            }
            out.flush();
            ByteBuffer[] bbufs = byteStore.toByteBuffers();
            byteStore.close();
            return Bufs.createBuf( bbufs,
                                   super.isBit64(), super.isBigendian() );
        }
    }

    public static CdfTableProfile createProfile( boolean invarParams,
                                                 String[] descripAttNames,
                                                 String[] unitAttNames ) {
        return new ListCdfTableProfile( invarParams, descripAttNames,
                                        unitAttNames );
    }

    private static class ListCdfTableProfile implements CdfTableProfile {
        private final boolean invarParams_;
        private final Collection<String> descAttNames_;
        private final Collection<String> unitAttNames_;

        ListCdfTableProfile( boolean invarParams, String[] descripAttNames,
                             String[] unitAttNames ) {
            invarParams_ = invarParams;
            descAttNames_ = toNormalisedList( descripAttNames );
            unitAttNames_ = toNormalisedList( unitAttNames );
        }

        public boolean invariantVariablesToParameters() {
            return invarParams_;
        }

        public String getDescriptionAttribute( String[] attNames ) {
            return match( attNames, descAttNames_ );
        }

        public String getUnitAttribute( String[] attNames ) {
            return match( attNames, unitAttNames_ );
        }

        private static Collection<String> toNormalisedList( String[] names ) {
            // Use a LinkedHashMap for its ordering properties.
            // Only keys are used, values are ignored.
            Map<String,Object> map = new LinkedHashMap<String,Object>();
            for ( int in = 0; in < names.length; in++ ) {
                map.put( normalise( names[ in ] ), null );
            }
            return map.keySet();
        }

        private static String match( String[] opts,
                                     Collection<String> targetList ) {
            for ( int i = 0; i < opts.length; i++ ) {
                String opt = opts[ i ];
                if ( targetList.contains( normalise( opt ) ) ) {
                    return opt;
                }
            }
            return null;
        }

        private static String normalise( String txt ) {
            return txt.trim().toLowerCase();
        }
    }
}
