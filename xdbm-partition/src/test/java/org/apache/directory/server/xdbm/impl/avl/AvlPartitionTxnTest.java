
package org.apache.directory.server.xdbm.impl.avl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.naming.directory.SearchControls;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.OperationExecutionManager;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.txn.TxnManager;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;

import org.junit.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvlPartitionTxnTest
{
    private static AvlPartition partition;
    private static SchemaManager schemaManager = null;

    
    /** Operation execution manager */
    private static OperationExecutionManager executionManager;

    /** Txn manager */
    private static TxnManager txnManager;
    
    /** log dir */
    private static File logDir;
    
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AvlPartitionTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }
        
        logDir = new File( workingDirectory + File.separatorChar + "txnlog" + File.separatorChar );
        logDir.mkdirs();
        TxnManagerFactory.init( logDir.getPath(), 1 << 13, 1 << 14 );
        OperationExecutionManagerFactory.init();
        executionManager = OperationExecutionManagerFactory.instance();
        txnManager = TxnManagerFactory.txnManagerInstance();

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }


    @Before
    public void createStore() throws Exception
    {
        // initialize the partition
        partition = new AvlPartition( schemaManager );
        partition.setId( "example" );
        partition.setSyncOnWrite( false );

        partition.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        partition.addIndex( new AvlIndex( SchemaConstants.UID_AT_OID ) );
        partition.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );

        partition.initialize();

        try
        {
            txnManager.beginTransaction( false );
            XdbmStoreUtils.loadExampleData( partition, schemaManager );
            txnManager.commitTransaction();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }
    }
   

    @After
    public void destroyStore() throws Exception
    {
        partition.destroy();
        
        if ( logDir != null )
        {
            FileUtils.deleteDirectory( logDir);
        }
    }
    
    
    @Test
    public void testAddsConcurrentWithSearch()
    {
        try
        {
            int numThreads = 10;
            AddsConcurrentWithSearchTestThread threads[] = new AddsConcurrentWithSearchTestThread[numThreads];
            
            
            for ( int idx =0; idx < numThreads; idx++ )
            {
                threads[idx] = new AddsConcurrentWithSearchTestThread();
                threads[idx].start();
            }
            
            txnManager.beginTransaction( false );
            
            // dn id 12
            Dn martinDn = new Dn( schemaManager, "cn=Marting King,ou=Sales,o=Good Times Co." );
            DefaultEntry entry = new DefaultEntry( schemaManager, martinDn );
            entry.add( "objectClass", "top", "person", "organizationalPerson" );
            entry.add( "ou", "Sales" );
            entry.add( "cn", "Martin King" );
            entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
            entry.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );

            AddOperationContext addContext = new AddOperationContext( null, entry );
            executionManager.add( partition, addContext );
            
            // Sleep some
            Thread.sleep( 100 );
            
            // dn id 13
            Dn jimmyDn = new Dn( schemaManager, "cn=Jimmy Wales, ou=Sales,o=Good Times Co." );
            entry = new DefaultEntry( schemaManager, jimmyDn );
            entry.add( "objectClass", "top", "person", "organizationalPerson" );
            entry.add( "ou", "Marketing" );
            entry.add( "cn", "Jimmy Wales" );
            entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
            entry.add( "entryUUID", Strings.getUUIDString( 13 ).toString() );
            
            addContext = new AddOperationContext( null, entry );
            executionManager.add( partition, addContext );
            
            txnManager.commitTransaction();
            
            for ( int idx =0; idx < numThreads; idx++ )
            {
                threads[idx].join();
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            assertTrue( false );
        }
    }
    

    private static boolean removeDirectory( File directory )
    {
        if ( directory == null )
        {
            return false;
        }
        
        if ( !directory.exists() )
        {
            return true;
        }
        
        if ( !directory.isDirectory() )
        {
            return false;   
        }
            

        String[] list = directory.list();
        
        if ( list != null )
        {
            for ( int i = 0; i < list.length; i++ )
            {
                File entry = new File( directory, list[i] );

                if ( entry.isDirectory() )
                {
                    if ( !removeDirectory( entry ) )
                        return false;
                }
                else
                {
                    if ( !entry.delete() )
                        return false;
                }
            }
        }

        return directory.delete();
    }
    
    class AddsConcurrentWithSearchTestThread extends Thread
    {
        private void doSearch() throws Exception
        {
            int numEntries = 0;
            
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
            ExprNode filter = new PresenceNode( schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT ) );
            
            Dn baseDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
            
            txnManager.beginTransaction( true );

            IndexCursor<UUID> cursor = partition.getSearchEngine().cursor( baseDn, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
            
            while ( cursor.next() )
            {
                numEntries++;
            }
            
            assertTrue( numEntries == 2 || numEntries == 4 );
            //System.out.println("Num entries: " + numEntries );
            
            txnManager.commitTransaction();
        }


        public void run()
        {         
            try
            {
                Random sleepRandomizer = new Random();
                int sleepTime = sleepRandomizer.nextInt( 10 ) * 100;
                
                Thread.sleep( sleepTime );
                
                doSearch();
            }
            catch( Exception e )
            {
                e.printStackTrace();
                fail();
                assertTrue( false );
            }
            
            
            
        }
    } // end of class RemoveInsertTestThread

}
