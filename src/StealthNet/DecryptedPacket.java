/* @formatter:off */
/******************************************************************************
 * ELEC5616
 * Computer and Network Security, The University of Sydney
 * Copyright (C) 2002-2004, Matt Barrie, Stephen Gould and Ryan Junee
 *
 * PACKAGE:         StealthNet
 * FILENAME:        DecryptedPacket.java
 * AUTHORS:         Matt Barrie, Stephen Gould, Ryan Junee and Joshua Spence
 * DESCRIPTION:     Implementation of a StealthNet packet. This class represents
 * 					decrypted packet contents. This class is more closely based
 * 					on the original 'Packet' class than the EncryptedPacket
 * 					class.
 *
 *****************************************************************************/
/* @formatter:on */

package StealthNet;

/* Import Libraries ******************************************************** */

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.management.InvalidAttributeValueException;

import org.apache.commons.codec.binary.Base64;

import StealthNet.Security.Encryption;
import StealthNet.Security.MessageAuthenticationCode;
import StealthNet.Security.NonceGenerator;

/* StealthNet.DecryptedPacket Class Definition ***************************** */

/**
 * A class to store the decrypted data passed between StealthNet {@link Client}
 * s. A StealthNet "packet" consists of three parts: <ul> <li>command</li>
 * <li>data</li> <li>nonce</li> </ul>
 * 
 * A nonce is generated by a pseudo-random random generator. The nonce is used
 * to prevent replay attacks, because once the packet is read at the receiving
 * end of the communications, the nonce is "consumed" and cannot be used again
 * for future communications. The DecryptedPacket class <em>will</em> allow
 * packets to be created without a nonce (if a null {@link NonceGenerator}
 * instance is passed to the function). A higher layer should check whether or
 * not this should be allowed.
 * 
 * @author Matt Barrie
 * @author Stephen Gould
 * @author Ryan Junee
 * @author Joshua Spence
 */
public class DecryptedPacket {
	/** The <ode>NULL</code> command is commonly used for acknowledgement. */
	public static final byte CMD_NULL = 0x00;
	public static final byte CMD_LOGIN = 0x01;
	public static final byte CMD_LOGOUT = 0x02;
	public static final byte CMD_MSG = 0x03;
	
	/**
	 * <b>NOTE:</b> Data will be of the form <code>user@host:port</code>.
	 */
	public static final byte CMD_CHAT = 0x04;
	
	/**
	 * <b>NOTE:</b> Data will be of the form
	 * <code>user@host:port#filename</code>.
	 */
	public static final byte CMD_FTP = 0x05;
	
	/**
	 * <b>NOTE:</b> Data will be of the form <code>userID;online\n...</code>
	 */
	public static final byte CMD_LIST = 0x06;
	public static final byte CMD_CREATESECRET = 0x07;
	public static final byte CMD_SECRETLIST = 0x08;
	
	/**
	 * <b>NOTE:</b> Data will be of the form <code>user@host:port</code>.
	 */
	public static final byte CMD_GETSECRET = 0x09;
	
	/**
	 * The <code>GETPUBLICKEY</code> command is used to request the
	 * {@link PublicKey} of a user from the server. <p> When the {@link Client}
	 * sends this command to the {@link Server}, the packet data contains the
	 * user ID of the user whose {@link PublicKey} is being requested. <p> When
	 * the {@link Server} sends this command to the {@link Client}, the packet
	 * data contains the {@link PublicKey} of the requested user, encoded in
	 * base-64.
	 * 
	 * @see Base64
	 */
	public static final byte CMD_GETPUBLICKEY = 0x0A;
	
	/*
	 * Payment-specific commands
	 */
	
	/**
	 * The {@link Client} sends a <code>CMD_PAYMENT</code> packet to the
	 * {@link Server} to add credit to their account. The contents of the packet
	 * is the number of credits being sent, as well as the relevant CryptoCredit
	 * hash. The data takes the form <code>credits;hash<code>. The hash is to be
	 * encoded in base-64. <p> If the {@link Client} wishes to cancel the
	 * transaction (possibly because they have insufficient funds in the
	 * {@link Bank}, then the {@link Client} client should send a "null"
	 * payment. <p> A {@link Client} can make a "payment" to the {@link Bank},
	 * in order to refund credits to the user's account.
	 */
	public static final byte CMD_PAYMENT = 0x40;
	
	/**
	 * The {@link Server} sends a <code>CMD_REQUESTPAYMENT</code> packet to the
	 * {@link Client} to request additional payment for a secret. The contents
	 * of the packet is the number of credits being requested.
	 */
	public static final byte CMD_REQUESTPAYMENT = 0x41;
	
	/**
	 * The {@link Client} sends a <code>CMD_SIGNHASHCHAIN</code> packet to the
	 * {@link Bank} to request that the bank sign a
	 * {@link CryptoCreditHashChain}. The contents of the packet is the
	 * identifier of the {@link CryptoCreditHashChain}, containing the user ID,
	 * the number of credits and the top CryptoCredit hash from the hash chain.
	 * The hash sent from the {@link Client} to the {@link Bank} is encoded in
	 * base-64. Similarly, the signature sent from the {@link Bank} to the
	 * {@link Client} is also encoded in base-64.
	 */
	public static final byte CMD_SIGNHASHCHAIN = 0x42;
	
	/**
	 * The {@link Client} sends a <code>CMD_GETBALANCE</code> packet to either
	 * the {@link Bank} or the {@link Server} to request the {@link Client}'s
	 * account balance.
	 */
	public static final byte CMD_GETBALANCE = 0x43;
	
	/**
	 * The {@link Server} sends a <code>CMD_VERIFYCREDIT</code> packet to the
	 * {@link Bank} when the {@link Client} sends a CryptoCredit hash for
	 * payment. The {@link Bank} sends back a response indicating whether or not
	 * the payment is valid.
	 * 
	 * The {@link Server} sends the data in the form
	 * <code>userID;credits;hash</code>. The hash is to be encoded in base-64.
	 * The {@link Bank} sends data in the form <code>[true|false]</code>.
	 */
	public static final byte CMD_VERIFYCREDIT = 0x44;
	
	/**
	 * The {@link Client} sends a <code>CMD_HASHCHAIN</code> packet to the
	 * {@link Server} upon generation of a new hash chain. The client must send
	 * both the identifier and the signature of the
	 * {@link CryptoCreditHashChain}. The packet data is encoded in base-64.
	 */
	public static final byte CMD_HASHCHAIN = 0x44;
	
	/*
	 * Security-specific commands - these should never be returned to a client
	 * or server, but rather should always be handled within the Comms class.
	 */
	
	/**
	 * The <code>CMD_AUTHENTICATIONKEY</code> command is used to share the
	 * common key for Diffie-Hellman key exchange.
	 */
	public static final byte CMD_AUTHENTICATIONKEY = 0x7A;
	
	/**
	 * The <code>CMD_INTEGRITYKEY</code> command is used to share the key for
	 * the MAC generator.
	 */
	public static final byte CMD_INTEGRITYKEY = 0x7B;
	
	/**
	 * The <code>CMD_NONCESEED</code> command is used to share the seed for the
	 * nonce generator.
	 */
	public static final byte CMD_NONCESEED = 0x7C;
	
	/**
	 * The <code>PUBLICKEY</code> command is used to share the asymmetric public
	 * key.
	 */
	public static final byte CMD_PUBLICKEY = 0x7D;
	
	/** The command being sent in the packet. */
	byte command;
	
	/** The data being sent in the packet. */
	final byte[] data;
	
	/** The pseudo-random nonce for this packet. */
	final byte[] nonce;
	
	/** The {@link MessageAuthenticationCode} used to provide a message digest. */
	final MessageAuthenticationCode mac;
	
	/** Null constructor with no digest and no nonce. */
	public DecryptedPacket() {
		command = DecryptedPacket.CMD_NULL;
		data = new byte[0];
		
		/** No nonce is available. */
		nonce = new byte[0];
		
		/** No MAC is available. */
		mac = null;
	}
	
	/**
	 * Constructor with no digest. Explicitly copies the data array contents.
	 * 
	 * @param cmd The command to be sent in the packet.
	 * @param d The data to be sent in the packet.
	 */
	public DecryptedPacket(final byte cmd, final byte[] d) {
		command = cmd;
		
		if (d == null)
			data = new byte[0];
		else {
			data = new byte[d.length];
			System.arraycopy(d, 0, data, 0, d.length);
		}
		
		/* No nonce is available. */
		nonce = new byte[0];
		
		/* No MAC is available. */
		mac = null;
	}
	
	/**
	 * Constructor with a digest and a nonce. Explicitly copies the data array
	 * contents.
	 * 
	 * @param cmd The command to be sent in the packet.
	 * @param dLen The length of the data array.
	 * @param d The data to be sent in the packet.
	 * @param mac The {@link MessageAuthenticationCode} instance to provide a
	 *        MAC digest.
	 * @param nonceGenerator The {@link NonceGenerator} instance to provide a
	 *        nonce.
	 */
	public DecryptedPacket(final byte cmd, final byte[] d, final int dLen, final MessageAuthenticationCode mac, final NonceGenerator nonceGenerator) {
		command = cmd;
		
		/* Copy the data. */
		if (d == null)
			data = new byte[0];
		else {
			data = new byte[dLen];
			System.arraycopy(d, 0, data, 0, dLen);
		}
		
		/* Create the nonce (if possible). */
		if (nonceGenerator != null)
			nonce = nonceGenerator.getNext();
		else
			nonce = new byte[0];
		
		this.mac = mac;
	}
	
	/**
	 * Constructor. This function must "undo" the effects of the
	 * <code>toString()</code> function, because this function converts the
	 * received data into a packet at the receiving end of communications.
	 * 
	 * @param str A {@link String} consisting of the packet contents.
	 */
	public DecryptedPacket(String str) {
		/*
		 * Add padding if necessary, to make the packet length an integer number
		 * of bytes (each represented by HEX_PER_BYTE hexadecimal characters).
		 */
		while (str.length() % Utility.HEX_PER_BYTE != 0)
			str = "0" + str;
		
		if (str.length() == 0) {
			/* NULL packet. */
			command = DecryptedPacket.CMD_NULL;
			data = new byte[0];
			nonce = null;
			mac = null;
		} else {
			/* Current index of the string. */
			int current = 0;
			
			/** Command (1 byte). */
			command = (byte) (16 * Utility.singleHexToInt(str.charAt(current++)) + Utility.singleHexToInt(str.charAt(current++)));
			
			/* Data length (4 bytes). */
			final int dataLen = Utility.hexToInt(str.substring(current, current + 4 * Utility.HEX_PER_BYTE));
			current += 4 * Utility.HEX_PER_BYTE;
			
			/* Data (dataLen bytes). */
			data = new byte[dataLen];
			for (int i = 0; i < data.length; i++)
				data[i] = (byte) (16 * Utility.singleHexToInt(str.charAt(current++)) + Utility.singleHexToInt(str.charAt(current++)));
			
			/* Nonce length (4 bytes). */
			final int nonceLen = Utility.hexToInt(str.substring(current, current + 4 * Utility.HEX_PER_BYTE));
			current += 4 * Utility.HEX_PER_BYTE;
			
			/* Nonce (nonceLen bytes). */
			nonce = new byte[nonceLen];
			for (int i = 0; i < nonce.length; i++)
				nonce[i] = (byte) (16 * Utility.singleHexToInt(str.charAt(current++)) + Utility.singleHexToInt(str.charAt(current++)));
			
			/* No MAC is available. */
			mac = null;
		}
	}
	
	/**
	 * Converts the packet to a string. This function must undo the effects of
	 * the <code>DecryptedPacket(String)</code> constructor, because this
	 * function is used to convert a packet to a string for transmission at the
	 * sending end of communications.
	 * 
	 * @return A string representing the contents of the packet.
	 */
	@Override
	public String toString() {
		String str = "";
		int lowHalfByte, highHalfByte;
		
		/* Command (1 byte). */
		highHalfByte = command >= 0 ? command : 256 + command;
		lowHalfByte = highHalfByte & 0xF;
		highHalfByte /= Utility.HEXTABLE.length;
		str += Utility.HEXTABLE[highHalfByte];
		str += Utility.HEXTABLE[lowHalfByte];
		
		/* Data length (4 bytes). */
		str += Utility.intToHex(data.length);
		
		/* Data (data.length / HEX_PER_BYTE bytes). */
		for (final byte element : data) {
			highHalfByte = element >= 0 ? element : 256 + element;
			lowHalfByte = highHalfByte & 0xF;
			highHalfByte /= Utility.HEXTABLE.length;
			str += Utility.HEXTABLE[highHalfByte];
			str += Utility.HEXTABLE[lowHalfByte];
		}
		
		/* Nonce length (4 bytes). */
		str += Utility.intToHex(nonce.length);
		
		/* Nonce (nonce.length / HEX_PER_BYTE bytes). */
		for (final byte element : nonce) {
			highHalfByte = element >= 0 ? element : 256 + element;
			lowHalfByte = highHalfByte & 0xF;
			highHalfByte /= Utility.HEXTABLE.length;
			str += Utility.HEXTABLE[highHalfByte];
			str += Utility.HEXTABLE[lowHalfByte];
		}
		
		/** Done. */
		return str;
	}
	
	/**
	 * Get the name of a command from its byte value. For debug purposes only.
	 * 
	 * @param command The byte value of the command to query the name of.
	 * @return A {@link String} containing the name of the command.
	 */
	public static String getCommandName(final byte command) {
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
			case CMD_GETPUBLICKEY:
				return "CMD_GETPUBLICKEY";
			case CMD_PAYMENT:
				return "CMD_PAYMENT";
			case CMD_REQUESTPAYMENT:
				return "CMD_REQUESTPAYMENT";
			case CMD_SIGNHASHCHAIN:
				return "CMD_SIGNHASHCHAIN";
			case CMD_GETBALANCE:
				return "CMD_GETBALANCE";
			case CMD_VERIFYCREDIT:
				return "CMD_VERIFYCREDIT";
			case CMD_AUTHENTICATIONKEY:
				return "CMD_AUTHENTICATIONKEY";
			case CMD_INTEGRITYKEY:
				return "CMD_INTEGRITYKEY";
			case CMD_NONCESEED:
				return "CMD_NONCESEED";
			case CMD_PUBLICKEY:
				return "CMD_PUBLICKEY";
			default:
				return "UNKNOWN";
		}
	}
	
	/**
	 * Get a {@link String} representation of the packet. For debug purposes
	 * only.
	 * 
	 * @return A comma-separated string containing the the value of each of the
	 *         packet's fields. For purely cosmetic purposes, newline characters
	 *         will be replaced by semicolons.
	 */
	public String getDecodedString() {
		String str = "";
		
		/** Packet name. */
		str += getCommandName(command);
		str += ", ";
		
		/** Packet data. */
		if (data.length > 0)
			str += new String(data).replaceAll("\n", ";");
		else
			str += "null";
		str += ", ";
		
		/** Packet nonce. */
		if (nonce != null && nonce.length > 0)
			str += Utility.getHexValue(nonce);
		else
			str += "null";
		
		return str;
	}
	
	/**
	 * Encrypt this packet.
	 * 
	 * @param e The {@link Encryption} instance to encrypt the packet. If null,
	 *        then the packet will not be encrypted.
	 * @return The {@link EncryptedPacket}.
	 * 
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws UnsupportedEncodingException
	 * @throws IllegalArgumentException
	 * @throws InvalidAttributeValueException
	 * 
	 * @see Encryption
	 * @see EncryptedPacket
	 */
	public EncryptedPacket encrypt(final Encryption e) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidAttributeValueException, IllegalArgumentException {
		if (e != null) {
			final byte[] encryptedData = e.encrypt(toString());
			return new EncryptedPacket(encryptedData, encryptedData.length, mac);
		} else
			return new EncryptedPacket(toString().getBytes(), toString().getBytes().length, mac);
	}
}

/******************************************************************************
 * END OF FILE: DecryptedPacket.java
 *****************************************************************************/
