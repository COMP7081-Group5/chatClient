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
        System.out.println("***NOTE***");
        System.out.println("These tests only test client side logic.");
        System.out.println("They assume they will receieve the correct response from the server based on their input.");
        System.out.println("**********");
        System.out.println("");
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

        System.out.println("********TESTING INPUT VALIDATION FOR NEW USER********");

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

        System.out.println("********TESTING INPUT VALIDATION FOR EDIT********");


        //why are these calling new user??
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
