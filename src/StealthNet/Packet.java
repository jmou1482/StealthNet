/***********************************************************************************
 * ELEC5616
 * Computer and Network Security, The University of Sydney
 * Copyright (C) 2002-2004, Matt Barrie, Stephen Gould and Ryan Junee
 *
 * PACKAGE:         StealthNet
 * FILENAME:        Packet.java
 * AUTHORS:         Matt Barrie, Stephen Gould and Ryan Junee
 * DESCRIPTION:     Implementation of StealthNet Communications for ELEC5616
 *                  programming assignment.
 *                  This code has been written for the purposes of teaching
 *                  cryptography and computer security. It is to be used as
 *                  a demonstration only. No attempt has been made to optimise
 *                  the source code.
 * VERSION:         1.0-ICE
 *
 * REVISION HISTORY:
 *
 **********************************************************************************/

package StealthNet;

/* Import Libraries **********************************************************/

import StealthNet.Security.MessageAuthenticationCode;

/* Packet Class Definition ***************************************************/

/**
 * A class to store the data passed between StealthNet clients. A StealthNet
 * "packet" consists of three parts:
 *     - command
 *     - data
 *     - token
 *     - digest
 *  
 *  A message digest is produced by passing a MessageAuthenticationCode instance
 *  to the relevant function. The StealthNet.Packet class will allow packets to 
 *  be created without a digest (if a null MessageAuthenticationCode instance is
 *  passed to the function). A higher layer should check whether or not this 
 *  should be allowed.
 *  
 *  A message token should be generated by a pseudo-random random generator.
 *  The message token is used to prevent replay attacks, because once the packet
 *  is read at the receiving end of the communications, the token is 'consumed'
 *  and cannot be used again for future communications. The StealthNet.Packet 
 *  class will allow packets to be created without a token (if a null 
 *  TokenGenerator instance is passed to the function). A higher layer should 
 *  check whether or not this should be allowed.
 * 
 * @author Matt Barrie
 * @author Stephen Gould
 * @author Ryan Junee
 * @author Joshua Spence (Added security-related commands. Also added 
 * getCommandName function for debug purposes.)
 */
public class Packet {
	/** Commands. */
    public static final byte CMD_NULL = 0x00;
    public static final byte CMD_LOGIN = 0x01;
    public static final byte CMD_LOGOUT = 0x02;
    public static final byte CMD_MSG = 0x03;
    public static final byte CMD_CHAT = 0x04;
    public static final byte CMD_FTP = 0x05;
    public static final byte CMD_LIST = 0x06;
    public static final byte CMD_CREATESECRET = 0x07;
    public static final byte CMD_SECRETLIST = 0x08;
    public static final byte CMD_GETSECRET = 0x09;
    
    /** Security-specific commands. */
    public static final byte CMD_PUBLICKEY = 0x0A;
    public static final byte CMD_INTEGRITYKEY = 0x0B;
    public static final byte CMD_TOKEN = 0x0C;
    
    /** Hexadecimal characters. */
    private static final char[] HEXTABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /** Packet contents. */
    byte command;			/** The command being sent in the packet. */      
    final byte data[];		/** The data being sent in the packet. */
    final Long token;		/** The pseudo-random token for this packet. */
    final byte[] digest;	/** The MAC digest of the packet data (in base64 encoding). */

    /** Null constructor with no digest. */
    public Packet() {
        this.command = CMD_NULL;
        this.data = new byte[0];
        
        /** No digest is available. */
        this.digest = new byte[0];
        
        /** No token is available. */
        token = null;
    }

    /** 
     * Constructor with no digest. Explicitly copies the data array contents.
     *
     * @param cmd The command to be sent in the packet.
     * @param d The data to be sent in the packet.
     */
    public Packet(byte cmd, byte[] d) {
        this.command = cmd;
        
        if (d == null)
        	this.data = new byte[0];
        else {
        	this.data = new byte[d.length];
        	System.arraycopy(d, 0, this.data, 0, d.length);
        }
        
        /** No digest is available. */
        this.digest = new byte[0];
        
        /** No token is available. */
        token = null;
    }
    
    /** 
     * Constructor with digest and token. Explicitly copies the data array 
     * contents.
     *
     * @param cmd The command to be sent in the packet.
     * @param dLen The length of the data array.
     * @param d The data to be sent in the packet.
     * @param mac The MessageAuthenticationCode instance to provide a MAC 
     * digest.
     */
    public Packet(byte cmd, byte[] d, int dLen, MessageAuthenticationCode mac, Long token) {
        this.command = cmd;
        
        /** Copy the data. */
        if (d == null)
        	this.data = new byte[0];
        else  {
        	this.data = new byte[dLen];
        	System.arraycopy(d, 0, this.data, 0, dLen);
        }
        
        /** Create the MAC digest (if possible). */
        if (mac == null)
        	this.digest = new byte[0];
        else
        	this.digest = mac.createMAC(getContents()).getBytes();
        
        /** Create the token. */
        this.token = token;
    }

    /** 
     * Constructor. This function must "undo" the effects of the toString() 
     * function, because this function converts the received data into a packet.
     * 
     * @param str A string consisting of the packet contents.
     */
    public Packet(String str) {
    	/** Add padding if necessary. */
    	if (str.length() % 2 == 1)
            str = "0" + str;
    	
        if (str.length() == 0) {
        	/** NULL packet. */
            this.command = CMD_NULL;
            this.data = new byte[0];
            this.digest = new byte[0];
            this.token = null;
        } else {
        	/** Current index of the string. */
        	int current = 0;
        	
        	/** Command (1 byte). */
            this.command = (byte) (16 * hexToInt(str.charAt(current++)) + hexToInt(str.charAt(current++)));
            
            /** Data length (2 bytes). */
            int dataLen = 4096 * hexToInt(str.charAt(current++)) + 
            		       256 * hexToInt(str.charAt(current++)) + 
            		        16 * hexToInt(str.charAt(current++)) + 
            		         1 * hexToInt(str.charAt(current++));
            
            /** Data (dataLen bytes). */
            this.data = new byte[dataLen];
            for (int i = 0; i < data.length; i++)
            	this.data[i] = (byte) (16 * hexToInt(str.charAt(current++)) + hexToInt(str.charAt(current++)));
            
            /** Token (8 bytes). */
            this.token = new Long(1152921504606846976L * (long) hexToInt(str.charAt(current++)) + 
            		                72057594037927936L * (long) hexToInt(str.charAt(current++)) + 
            		                 4503599627370496L * (long) hexToInt(str.charAt(current++)) + 
            		                  281474976710656L * (long) hexToInt(str.charAt(current++)) + 
            		                   17592186044416L * (long) hexToInt(str.charAt(current++)) + 
            		                    1099511627776L * (long) hexToInt(str.charAt(current++)) + 
            		                      68719476736L * (long) hexToInt(str.charAt(current++)) + 
            		                       4294967296L * (long) hexToInt(str.charAt(current++)) + 
            		                        268435456L * (long) hexToInt(str.charAt(current++)) + 
            		                         16777216L * (long) hexToInt(str.charAt(current++)) + 
            		                          1048576L * (long) hexToInt(str.charAt(current++)) + 
            	                                65536L * (long) hexToInt(str.charAt(current++)) + 
            		                             4096L * (long) hexToInt(str.charAt(current++)) + 
            		                              256L * (long) hexToInt(str.charAt(current++)) + 
            		                               16L * (long) hexToInt(str.charAt(current++)) + 
            		                                1L * (long) hexToInt(str.charAt(current++)));
            
            /** Digest length (2 bytes). */
            int digestLen = 4096 * hexToInt(str.charAt(current++)) + 
            		         256 * hexToInt(str.charAt(current++)) + 
            		          16 * hexToInt(str.charAt(current++)) + 
            		           1 * hexToInt(str.charAt(current++));
            
            /** Digest (digestLen bytes). */
            this.digest = new byte[digestLen];
            for (int i = 0; i < digest.length; i++)
            	this.digest[i] = (byte) (16 * hexToInt(str.charAt(current++)) + hexToInt(str.charAt(current++)));
        }
    }
    
    /** 
     * Gets a string containing everything except for the MAC digest. Used to
     * compute the MAC digest.
     * 
     * @return A string representing the contents of the packet.
     */
    private String getContents() {
        String str = "";
        int lowHalfByte, highHalfByte;

        /** Command (1 byte).  */
        highHalfByte = (command >= 0) ? command : (256 + command);
        lowHalfByte = highHalfByte & 0xF;
        highHalfByte /= 16;
        str += HEXTABLE[highHalfByte];
        str += HEXTABLE[lowHalfByte];
        
        /** Data length (2 bytes). */
        final int dataLenHi = (data.length / 256);
        final int dataLenLo = (data.length % 256);
        
        /* First byte */
        highHalfByte = (dataLenHi >= 0) ? dataLenHi : (256 + dataLenHi);
        lowHalfByte = highHalfByte & 0xF;
        highHalfByte /= 16;
        str += HEXTABLE[highHalfByte];
        str += HEXTABLE[lowHalfByte];
        
        /* Second byte */
        highHalfByte = (dataLenLo >= 0) ? dataLenLo : (256 + dataLenLo);
        lowHalfByte = highHalfByte & 0xF;
        highHalfByte /= 16;
        str += HEXTABLE[highHalfByte];
        str += HEXTABLE[lowHalfByte];
        
        /** Data (dataLen bytes). */
        for (int i = 0; i < data.length; i++) {
        	highHalfByte = (data[i] >= 0) ? data[i] : 256 + data[i];
        	lowHalfByte = highHalfByte & 0xF;
            highHalfByte /= 16;
            str += HEXTABLE[highHalfByte];
            str += HEXTABLE[lowHalfByte];
        }
        
        /** Token (8 bytes). */
        long t;
        if (token == null)
        	t = 0L;
        else
        	t = token.longValue();
        long divisor = 1152921504606846976L;
        for (int i = 0; i < 15; i++) {
        	final int current = (int) (t / divisor);
        	t = t % divisor;
        	divisor /= 16;
        	
        	str += HEXTABLE[current];
        }
        str += HEXTABLE[(int) t];
        
        return str;
    }

    /** 
     * Converts the packet to a string. This function must undo the effects of 
     * the StealthNet.Packet(String) constructor, because this function is used
     * to convert a packet to a string for transmission.
     * 
     * @return A string representing the contents of the packet.
     */
    public String toString() {
        String str = getContents();
        int lowHalfByte, highHalfByte;
        
        /** MAC Digest length (2 bytes) */
        final int digestLenHi = (digest.length / 256);
        final int digestLenLo = (digest.length % 256);
        
        /* First byte */
        highHalfByte = (digestLenHi >= 0) ? digestLenHi : (256 + digestLenHi);
        lowHalfByte = highHalfByte & 0xF;
        highHalfByte /= 16;
        str += HEXTABLE[highHalfByte];
        str += HEXTABLE[lowHalfByte];
        
        /* Second byte */
        highHalfByte = (digestLenLo >= 0) ? digestLenLo : (256 + digestLenLo);
        lowHalfByte = highHalfByte & 0xF;
        highHalfByte /= 16;
        str += HEXTABLE[highHalfByte];
        str += HEXTABLE[lowHalfByte];
        
        /** MAC Digest (digestLen bytes). */
        for (int i = 0; i < digest.length; i++) {
        	highHalfByte = (digest[i] >= 0) ? digest[i] : (256 + digest[i]);
        	lowHalfByte = highHalfByte & 0xF;
            highHalfByte /= 16;
            str += HEXTABLE[highHalfByte];
            str += HEXTABLE[lowHalfByte];
        }

        return str;
    }
    
    /**
     * Verify a MAC digest by calculating our own MAC digest of the same data,
     * and comparing the two. If the MessageAuthenticationCode instance is null,
     * then the verification will pass automatically.
     * 
     * @param mac The MessageAuthenticationCode instance to calculate the MAC 
     * digest.
     * @return True if the digest matches (or if the MessageAuthenticationCode 
     * instance is null), otherwise false.
     */
    public boolean verifyMAC(MessageAuthenticationCode mac) {
    	if (mac == null)
    		return true;
    	else 
    		return mac.verifyMAC(getContents(), digest);
    }

    /** 
     * A utility function to convert hexadecimal numbers to decimal integers.
     * 
     * @param hex The hexadecimal character to convert to an integer.
     * @return The integer value of the hexadecimal character.
     */
    private static int hexToInt(char hex) {
             if ((hex >= '0') && (hex <= '9')) return (hex - '0');
        else if ((hex >= 'A') && (hex <= 'F')) return (hex - 'A' + 10);
        else if ((hex >= 'a') && (hex <= 'f')) return (hex - 'a' + 10);
        else return 0;
    }
    
    /**
     * Get the name of a command from its byte value. For debug purposes only.
     * 
     * @param command The byte value of the command to query the name of.
     * @return A String containing the name of the command.
     */
    public static String getCommandName(byte command) {
    	switch (command) {
    		case CMD_NULL:
				return "CMD_NULL";
    		case CMD_LOGIN:
    			return "CMD_LOGIN";
    		case CMD_LOGOUT:
    			return "CMD_LOGOUT";
    		case CMD_MSG:
				return "CMD_MSG";
    		case CMD_CHAT:
    			return "CMD_CHAT";
    		case CMD_FTP:
    			return "CMD_FTP";
    		case CMD_LIST:
    			return "CMD_LIST";
    		case CMD_CREATESECRET:
    			return "CMD_CREATESECRET";
    		case CMD_SECRETLIST:
				return "CMD_SECRETLIST";
    		case CMD_GETSECRET:
    			return "CMD_GETSECRET";
    		case CMD_PUBLICKEY:
				return "CMD_PUBLICKEY";
    		case CMD_INTEGRITYKEY:
				return "CMD_INTEGRITYKEY";
    		case CMD_TOKEN:
    			return "CMD_TOKEN";
			default:
				return "UNKNOWN";
    	}
    }
}

/******************************************************************************
 * END OF FILE:     Packet.java
 *****************************************************************************/