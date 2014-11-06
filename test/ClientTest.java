/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author callum
 * none of the tests actually work
 */
public class ClientTest {
    
    public ClientTest() {

    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testPrintMessage() {

	}

    @Test
    public void testNewUser() {
        Client c = new Client("", 1500);

        System.out.println("TEST: Using invalid username");
        assertEquals("Username is not valid", false, c.newUser("","",-1,-1));

        System.out.println("TEST: Using Valid username, invalid Password");
        assertEquals("Username is not valid", false, c.newUser("Ben","",-1,-1));

        
    }


}
