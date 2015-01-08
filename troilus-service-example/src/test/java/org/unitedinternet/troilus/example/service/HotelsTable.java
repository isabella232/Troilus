package org.unitedinternet.troilus.example.service;

import com.unitedinternet.troilus.Schema;



public interface HotelsTable  {
   
    public static final String TABLE = "hotels";
    
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ROOM_IDS = "room_ids";
    public static final String DESCRIPTION = "description";
    public static final String CLASSIFICATION = "classification";
    
    
    public static final String CREATE_STMT = Schema.load("com/unitedinternet/troilus/example/service/hotels.ddl");
}
