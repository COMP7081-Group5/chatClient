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

        System.out.println("******** TESTING ADDING A NEW USER ********");

        System.out.println("TEST: Using invalid username");
        assertEquals("Username is not valid", false, c.newUser("","",-1,-1));

        System.out.println("TEST: Using Valid username, invalid Password");
        assertEquals("Username is not valid", false, c.newUser("Ben","",-1,-1));

        System.out.println("TEST: Using Valid username, Valid Password, Invalid usertype");
        assertEquals("Username is not valid", false, c.newUser("Ben","BenPassword",-1,-1));

        System.out.println("TEST: Using Valid username, Valid Password, Valid usertype, Invalid teamid");
        assertEquals("Username is not valid", false, c.newUser("Ben","BenPassword",0,-1));

        System.out.println("TEST: Using Valid username, Valid Password, Valid usertype, Valid teamid");
        assertEquals("Username is not valid", true, c.newUser("Ben","BenPassword",0,7));

        System.out.println("");

    }


    @Test
    public void testEdit() {
        Client c = new Client("", 1500);

        System.out.println("******** TESTING EDITING A USER ********");

        System.out.println("TEST: Using invalid username");
        assertEquals("Username is not valid", false, c.newUser("","",-1,-1));

        System.out.println("TEST: Using Valid username, invalid Password");
        assertEquals("Username is not valid", false, c.newUser("Ben","",-1,-1));

        System.out.println("TEST: Using Valid username, Valid Password, Invalid usertype");
        assertEquals("Username is not valid", false, c.newUser("Ben","BenPassword",-1,-1));

        System.out.println("TEST: Using Valid username, Valid Password, Valid usertype");
        assertEquals("Username is not valid", true, c.newUser("Ben","BenPassword",0,7));

        System.out.println("");
    }
}
