/**
 * 
 */
package net.oneandone.troilus.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import net.oneandone.troilus.CassandraDB;
import net.oneandone.troilus.Dao;
import net.oneandone.troilus.DaoImpl;
import net.oneandone.troilus.Field;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;



/**
 * Tests ability to map to an entity with inherited @Field(s)
 * 
 * @author Jason Westra  12-13-2015
 * 
 *
 */
public class EntityInheritanceMappingTest {

	private static CassandraDB cassandra;
	 
	Session session;
	
	public static final String TABLE_MOCK_WITH_INHERITANCE = "mock_with_inheritance";
		
	@BeforeClass
    public static void beforeClass() throws IOException {
        cassandra = CassandraDB.newInstance();
		createTables();	
    }
        
    @AfterClass
    public static void afterClass() throws IOException {
        cassandra.close();
    }
	    
	@Before
	public void setUp() throws Exception {
		session = cassandra.getSession();
	}
		
	private static void createTables() {
		Session session = cassandra.getSession();
		
		session.execute("CREATE TABLE " + TABLE_MOCK_WITH_INHERITANCE + " (id text, version bigint, create_date timestamp, latitude decimal, PRIMARY KEY (id));");
	}

	@Test
	public void testEntityWithInheritance() {
		
		Dao dao = new DaoImpl(session, TABLE_MOCK_WITH_INHERITANCE);
				
		MockDOWithInheritance mockDOWithInheritance = new MockDOWithInheritance();
		mockDOWithInheritance.setCreateDate(new Date());
		mockDOWithInheritance.setId("some_id");
		mockDOWithInheritance.setVersion(1);
		mockDOWithInheritance.setLatitude(new BigDecimal("23"));
		
		// insert here
		dao.writeEntity(mockDOWithInheritance)
			.ifNotExists()
			.execute();
		
		com.datastax.driver.core.ResultSet rs =session.execute("select * from " + TABLE_MOCK_WITH_INHERITANCE);
		if (rs.one() == null) fail("Failed to insert the row");
		
		mockDOWithInheritance.setLatitude(new BigDecimal(44));
		mockDOWithInheritance.setVersion(2);
		
		// update here demonstrating onlyIf() on the version
		try {
			dao.writeWithKey("id", mockDOWithInheritance.getId())
			.entity(mockDOWithInheritance)
			.onlyIf(QueryBuilder.eq("version", mockDOWithInheritance.getVersion() - 1))
			.execute();
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		// find one here
		MockDOWithInheritance afterUpdate = dao.readWithKey("id", mockDOWithInheritance.getId())
			.asEntity(MockDOWithInheritance.class)
			.execute().get();
		
		assertEquals("Did not update version", 2, afterUpdate.getVersion());
		assertEquals("Did not update latitude", mockDOWithInheritance.getLatitude(), afterUpdate.getLatitude());
		
		dao.deleteWithKey("id", mockDOWithInheritance.getId()).execute();
		
		Optional<MockDOWithInheritance> afterDelete = dao.readWithKey("id", mockDOWithInheritance.getId())
				.asEntity(MockDOWithInheritance.class)
				.execute();
		
		assertFalse("Delete did not work", afterDelete.isPresent());
	}
	
	// Tests @Field shows up on subclasses
	abstract public static class AbstractDO {
		
		@Field(name="id")
		private String id;
				
		@Field(name="create_date")
		private Date createDate;
				
		@Field(name="version")
		private long version;

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}

		/**
		 * @return the createDate
		 */
		public Date getCreateDate() {
			return createDate;
		}

		/**
		 * @param createDate the createDate to set
		 */
		public void setCreateDate(Date createDate) {
			this.createDate = createDate;
		}

		/**
		 * @return the version
		 */
		public long getVersion() {
			return version;
		}

		/**
		 * @param version the version to set
		 */
		public void setVersion(long version) {
			this.version = version;
		}
	}
	
	// Inherits to get common fields like id, audit, and version
	public static class MockDOWithInheritance extends AbstractDO {
		
		@Field(name="latitude")
		private BigDecimal latitude;

		/**
		 * @return the latitude
		 */
		public BigDecimal getLatitude() {
			return latitude;
		}

		/**
		 * @param latitude the latitude to set
		 */
		public void setLatitude(BigDecimal latitude) {
			this.latitude = latitude;
		}
	}
	
	
	
	
}
