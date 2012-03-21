/***********************************************************************************
 * ELEC5616
 * Computer and Network Security, The University of Sydney
 *
 * PROJECT:         StealthNet
 * FILENAME:        StealthNetComms.java
 * AUTHORS:         Stephen Gould, Matt Barrie, Ryan Junee
 * DESCRIPTION:     Implementation of StealthNet Communications for ELEC5616
 *                  programming assignment.
 *                  This code has been written for the purposes of teaching
 *                  cryptography and computer security. It is to be used as
 *                  a demonstration only. No attempt has been made to optimise
 *                  the source code.
 * VERSION:         1.0
 * IMPLEMENTS:      initiateSession();
 *                  acceptSession();
 *                  terminateSession();
 *                  sendPacket();
 *                  recvPacket();
 *                  recvReady();
 *
 * REVISION HISTORY:
 *
 **********************************************************************************/

package StealthNet;

/* Import Libraries **********************************************************/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/* StealthNetComms class *****************************************************/

/**
 * TODO
 * 
 * @author Stephen Gould
 * @author Matt Barrie
 * @author Ryan Junee
 *
 */
public class StealthNetComms {
	private static final boolean DEBUG = false;
	
    public static final String SERVERNAME = "localhost";
    public static final int SERVERPORT = 5616;

    /** Communications socket. */
    private Socket commsSocket;

    /** Output data stream. */
    private PrintWriter dataOut;            
    
    /** Input data stream */
    private BufferedReader dataIn;         

    /** Constructor */
    public StealthNetComms() {
        commsSocket = null;
        dataIn = null;
        dataOut = null;
    }

    /** Cleans up before terminating the class. */
    protected void finalize() throws IOException {
        if (dataOut != null) {
            dataOut.close();
        }
        if (dataIn != null) {
            dataIn.close();
        }
        if (commsSocket != null) {
            commsSocket.close();
        }
    }

    public boolean initiateSession(Socket socket) {
        try {
            commsSocket = socket;
            dataOut = new PrintWriter(commsSocket.getOutputStream(), true);
            dataIn = new BufferedReader(new InputStreamReader(commsSocket.getInputStream()));
        } catch (Exception e) {
            System.err.println("Connection terminated.");
            System.exit(1);
        }

        return true;
    }

    public boolean acceptSession(Socket socket) {
        try {
            commsSocket = socket;
            dataOut = new PrintWriter(commsSocket.getOutputStream(), true);
            dataIn = new BufferedReader(new InputStreamReader(commsSocket.getInputStream()));
        } catch (Exception e) {
            System.err.println("Connection terminated.");
            System.exit(1);
        }

        return true;
    }

    public boolean terminateSession() {
        try {
            if (commsSocket == null)
                return false;
            dataIn.close();
            dataOut.close();
            commsSocket.close();
            commsSocket = null;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean sendPacket(byte command) {
        return sendPacket(command, new byte[0]);
    }

    public boolean sendPacket(byte command, String data) {
        System.out.println("String data: " + data);
        return sendPacket(command, data.getBytes());
    }

    public boolean sendPacket(byte command, byte[] data) {
        return sendPacket(command, data, data.length);
    }

    public boolean sendPacket(byte command, byte[] data, int size) {
        StealthNetPacket pckt = new StealthNetPacket();
        pckt.command = command;
        pckt.data = new byte[size];
        System.arraycopy(data, 0, pckt.data, 0, size);
        return sendPacket(pckt);
    }

    public boolean sendPacket(StealthNetPacket pckt) {
        if (dataOut == null)
            return false;
        dataOut.println(pckt.toString());
        return true;
    }

    public StealthNetPacket recvPacket() throws IOException {
        StealthNetPacket pckt = null;
        String str = dataIn.readLine();
        pckt = new StealthNetPacket(str);
        return pckt;
    }

    /**
     * TODO  
     * 
     * @return True to indicate ready. False to indicate not-ready.
     * @throws IOException
     */
    public boolean recvReady() throws IOException {
    	if (DEBUG) {
	        System.out.println("Connected: " + commsSocket.isConnected());
	        System.out.println("Closed: " + commsSocket.isClosed());
	        System.out.println("InClosed: " + commsSocket.isInputShutdown());
	        System.out.println("OutClosed: " + commsSocket.isOutputShutdown());
    	}
        return dataIn.ready();
    }
}

/******************************************************************************
 * END OF FILE:     StealthNetComms.java
 *****************************************************************************/