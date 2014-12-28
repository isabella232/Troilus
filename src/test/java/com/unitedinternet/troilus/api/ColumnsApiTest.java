package com.unitedinternet.troilus.api;




import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.unitedinternet.troilus.AbstractCassandraBasedTest;
import com.unitedinternet.troilus.IfConditionException;
import com.unitedinternet.troilus.Dao;
import com.unitedinternet.troilus.DaoManager;
import com.unitedinternet.troilus.Mutation;
import com.unitedinternet.troilus.Record;
import com.unitedinternet.troilus.Result;


public class ColumnsApiTest extends AbstractCassandraBasedTest {
    

        
    
    @Test
    public void testSimpleTable() throws Exception {
        DaoManager daoManager = new DaoManager(getSession());

        Dao usersDao = daoManager.getDao(UsersTable.TABLE)
                                 .withConsistency(ConsistencyLevel.LOCAL_QUORUM);

        


        
        ////////////////
        // inserts
        usersDao.writeWithKey(UsersTable.USER_ID, "95454")
                .value(UsersTable.IS_CUSTOMER, true) 
                .value(UsersTable.PICTURE, ByteBuffer.wrap(new byte[] { 8, 4, 3})) 
                .value(UsersTable.ADDRESSES, ImmutableList.of("stuttgart", "baden-baden")) 
                .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("34234243", "9345324"))
                .execute();
        
        
        ExecutionInfo info =  usersDao.writeWithKey(UsersTable.USER_ID, "8345345")
                                      .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("24234244"))
                                      .value(UsersTable.IS_CUSTOMER, true)
                                      .ifNotExits()
                                      .withTtl(Duration.ofMinutes(2))
                                      .withWritetime(Instant.now().toEpochMilli() * 1000)
                                      .withEnableTracking()
                                      .execute()
                                      .getExecutionInfo();

        Assert.assertNotNull(info.getQueryTrace());
        
        
        
        Result result = usersDao.writeWithKey(UsersTable.USER_ID, "4545")
                                .value(UsersTable.IS_CUSTOMER, true)
                                .value(UsersTable.PICTURE, ByteBuffer.wrap(new byte[] { 4, 5, 5}))
                                .value(UsersTable.ADDRESSES, ImmutableList.of("m�nchen", "karlsruhe"))
                                .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("94665", "34324543"))
                                .ifNotExits()
                                .withEnableTracking()
                                .execute();


        try {   // insert twice!
            usersDao.writeWithKey(UsersTable.USER_ID, "4545")
                    .value(UsersTable.IS_CUSTOMER, true)
                    .value(UsersTable.PICTURE, ByteBuffer.wrap(new byte[] { 4, 5, 5}))
                    .value(UsersTable.ADDRESSES, ImmutableList.of("m�nchen", "karlsruhe"))
                    .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("94665", "34324543"))
                    .ifNotExits()       
                    .execute();
            
            Assert.fail("DuplicateEntryException expected"); 
        } catch (IfConditionException expected) { }  

        
        
        usersDao.writeWithKey(UsersTable.USER_ID, "3434343")
                .value(UsersTable.IS_CUSTOMER, Optional.of(true))
                .value(UsersTable.PICTURE, ByteBuffer.wrap(new byte[] { 4, 5, 5}))
                .value(UsersTable.ADDRESSES, null)
                .execute();


        
        ////////////////
        // reads
        Optional<Record> optionalRecord = usersDao.readWithKey(UsersTable.USER_ID, "4545")
                                                  .column(UsersTable.PICTURE)
                                                  .column(UsersTable.ADDRESSES)
                                                  .column(UsersTable.PHONE_NUMBERS)
                                                  .execute();
        Assert.assertTrue(optionalRecord.isPresent());
        optionalRecord.ifPresent(record -> System.out.println(record.getList(UsersTable.ADDRESSES, String.class).get()));
        System.out.println(optionalRecord.get());
        
        
        
        Optional<Record> optionalRecord2 = usersDao.readWithKey(UsersTable.USER_ID, "95454")
                                                   .columns(ImmutableList.of(UsersTable.PICTURE, UsersTable.ADDRESSES, UsersTable.PHONE_NUMBERS))
                                                   .execute();
        Assert.assertTrue(optionalRecord2.isPresent());
        optionalRecord2.ifPresent(record -> System.out.println(record.getList(UsersTable.ADDRESSES, String.class).get()));

 
        
        Optional<Record> optionalRecord3 = usersDao.readWithKey(UsersTable.USER_ID, "8345345")
                                                   .columnWithMetadata(UsersTable.IS_CUSTOMER)
                                                   .column(UsersTable.PICTURE)
                                                   .execute();
        Assert.assertTrue(optionalRecord3.isPresent());
        optionalRecord3.ifPresent(record -> System.out.println(record));

 
       
        

        ////////////////
        // deletes
        usersDao.deleteWithKey(UsersTable.USER_ID, "4545")
               .execute();
        
        // check
        optionalRecord = usersDao.readWithKey(UsersTable.USER_ID, "4545")
                                 .column(UsersTable.USER_ID)
                                 .execute();
        Assert.assertFalse(optionalRecord.isPresent());
        

        
        
        
        
        
        ////////////////
        // batch inserts
        Mutation<?> insert1 = usersDao.writeWithKey(UsersTable.USER_ID, "14323425")
                                      .value(UsersTable.IS_CUSTOMER, true)
                                      .value(UsersTable.ADDRESSES, ImmutableList.of("berlin", "budapest"))
                                      .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("12313241243", "232323"));
        
        
        Mutation<?> insert2 = usersDao.writeWithKey(UsersTable.USER_ID, "2222")
                                      .value(UsersTable.IS_CUSTOMER, true)
                                      .value(UsersTable.ADDRESSES, ImmutableList.of("berlin", "budapest"))
                                      .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("12313241243", "232323"));
        
        usersDao.writeWithKey(UsersTable.USER_ID, "222222")
                .value(UsersTable.IS_CUSTOMER, true)
                .value(UsersTable.ADDRESSES, ImmutableList.of("hamburg"))
                .value(UsersTable.PHONE_NUMBERS, ImmutableSet.of("945453", "23432234"))
                .combinedWith(insert1)
                .combinedWith(insert2)
                .withLockedBatchType()
                .execute();
        
        
        // check
        optionalRecord = usersDao.readWithKey(UsersTable.USER_ID, "14323425")
                                 .all()
                                 .execute();
        System.out.println(optionalRecord);
        Assert.assertTrue(optionalRecord.isPresent());
        
        
        
        
        
        
        
        
        
        Record record = usersDao.readWithKey(UsersTable.USER_ID, "8345345")
                                .execute()
                                .get();
        Assert.assertEquals("8345345", record.getString(UsersTable.USER_ID).get());
        Assert.assertEquals(true, record.getBool(UsersTable.IS_CUSTOMER).get());
        
        Iterator<String> phoneNumbers = record.getSet(UsersTable.PHONE_NUMBERS, String.class).get().iterator();
        Assert.assertEquals("24234244", phoneNumbers.next());
        Assert.assertFalse(phoneNumbers.hasNext());
        
        
        // remove value
        usersDao.writeWithKey(UsersTable.USER_ID, "8345345")
                .value(UsersTable.IS_CUSTOMER, null)
                .execute();
        
    
        record = usersDao.readWithKey(UsersTable.USER_ID, "8345345")
                         .execute()
                         .get();
        Assert.assertEquals("8345345", record.getString(UsersTable.USER_ID).get());
        Assert.assertFalse(record.getBool(UsersTable.IS_CUSTOMER).isPresent());

        phoneNumbers = record.getSet(UsersTable.PHONE_NUMBERS, String.class).get().iterator();
        Assert.assertEquals("24234244", phoneNumbers.next());
        Assert.assertFalse(phoneNumbers.hasNext());
        
        
        
        
        

        
        ////////////////////
        // conditional update

        
        try {
            usersDao.writeWithKey(UsersTable.USER_ID, "2222")
                    .value(UsersTable.ADDRESSES, ImmutableList.of("n�rnberg"))
                    .onlyIf(QueryBuilder.eq(UsersTable.IS_CUSTOMER, false))
                    .withSerialConsistency(ConsistencyLevel.SERIAL)
                    .execute();
            Assert.fail("IfConditionException expected");
        } catch (IfConditionException expected) {  }

        record = usersDao.readWithKey(UsersTable.USER_ID, "2222")
                .execute()
                .get();
        
        Iterator<String> addresses= record.getList(UsersTable.ADDRESSES, String.class).get().iterator();
        Assert.assertEquals("berlin", addresses.next());
        Assert.assertEquals("budapest", addresses.next());
        Assert.assertFalse(addresses.hasNext());        

        
        
        usersDao.writeWithKey(UsersTable.USER_ID, "2222")
                .value(UsersTable.ADDRESSES, ImmutableList.of("n�rnberg"))
                .onlyIf(QueryBuilder.eq(UsersTable.IS_CUSTOMER, true))
                .execute();

        record = usersDao.readWithKey(UsersTable.USER_ID, "2222")
                .execute()
                .get();
        
        addresses= record.getList(UsersTable.ADDRESSES, String.class).get().iterator();
        Assert.assertEquals("n�rnberg", addresses.next());
        Assert.assertFalse(addresses.hasNext());   
        
        
        
        
        
        usersDao.writeWhere(QueryBuilder.in(UsersTable.USER_ID, "2222"))
                .value(UsersTable.ADDRESSES, ImmutableList.of("berlin", "budapest"))
                .execute();
        
        record = usersDao.readWithKey(UsersTable.USER_ID, "2222")
                .execute()
                .get();
        
        addresses= record.getList(UsersTable.ADDRESSES, String.class).get().iterator();
        Assert.assertEquals("berlin", addresses.next());
        Assert.assertEquals("budapest", addresses.next());
        Assert.assertFalse(addresses.hasNext());        
      }    
    
}


