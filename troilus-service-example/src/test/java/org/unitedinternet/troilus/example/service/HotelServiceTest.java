package org.unitedinternet.troilus.example.service;

import java.io.File;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;






import org.apache.catalina.startup.Tomcat;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.junit.Assert;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.google.common.collect.ImmutableSet;
import com.unitedinternet.troilus.Dao;
import com.unitedinternet.troilus.DaoImpl;

public class HotelServiceTest extends AbstractCassandraBasedTest {
    
    
        
    @Test
    public void testExample() throws Exception {
        filldb();
        
        Tomcat server = new Tomcat();
        server.setPort(9080);
        server.addWebapp("/service", new File("src/main/resources/webapp").getAbsolutePath());
        server.start();
        
        Client client =  ResteasyClientBuilder.newClient();

        
        HotelRepresentation hotel = client.target("http://localhost:9080/service/hotels/BUP45544")
                                          .request()
                                          .get(HotelRepresentation.class);
        Assert.assertEquals("Corinthia Budapest", hotel.getName());
    }        

    
    
    
    private void filldb() {
        Dao hotelsDao = new DaoImpl(getSession(), HotelsTable.TABLE);
        
        hotelsDao.writeEntity(new Hotel("BUP45544", 
                                        "Corinthia Budapest",
                                        ImmutableSet.of("1", "2", "3", "122", "123", "124", "322", "333"),
                                        Optional.of(5), 
                                        Optional.of("Superb hotel housed in a heritage building - exudes old world charm")
                                       ))
                 .ifNotExits()
                 .execute();
    }
}


