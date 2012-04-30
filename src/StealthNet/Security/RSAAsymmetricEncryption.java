/******************************************************************************
 * ELEC5616
 * Computer and Network Security, The University of Sydney
 *
 * PACKAGE:         StealthNet.Security
 * FILENAME:        RSAAsymmetricEncryption.java
 * AUTHORS:         Joshua Spence
 * DESCRIPTION:     A class to provide RSA asymmetric encryption.
 *
 *****************************************************************************/

package StealthNet.Security;

/* Import Libraries **********************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.LinkedList;
import java.util.Queue;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;

import StealthNet.EncryptedFile;

/* StealthNet.Security.AsymmetricEncryption Interface Definition *************/

/**
 * A class to provide RSA asymmetric encryption. Encryption will be performed 
 * using the peer's public key. Decryption will be performed using our private 
 * key. Also provides the option to save the public key to an unencrypted file 
 * and the private file to a password-protected file
 *
 * @author Joshua Spence
 */
public class RSAAsymmetricEncryption implements AsymmetricEncryption {
	/** Keys. */
	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private PublicKey peerPublicKey;
	
	/** Ciphers. */
	private Cipher encryptionCipher;
	private final Cipher decryptionCipher;
	
	/** Constants describing the algorithm. */
	public static final String CIPHER_ALGORITHM = "RSA";
	public static final String ALGORITHM = CIPHER_ALGORITHM;
	public static final int NUM_BITS = 2048;
	private static final int MAX_CLEARTEXT = (NUM_BITS / Byte.SIZE) - 11;
	private static final int MAX_CIPHERTEXT = 256;
	
	/**
	 * Constructor to generate a new public/private key pair.
	 * 
	 * @param peer The public key of the peer of the communications, used for 
	 * encryption. If null, then encryption will be unavailable.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchProviderException 
	 */
	public RSAAsymmetricEncryption(PublicKey peer) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, NoSuchProviderException {		
		/** Initialise the key generator. */
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
		kpg.initialize(NUM_BITS);
		
		/** Establish the keys. */
		final KeyPair kp = kpg.genKeyPair();
		this.publicKey = kp.getPublic();
		this.privateKey = kp.getPrivate();
		
		/** Initialise the encryption cipher. */
		setPeerPublicKey(peer);
		
		/** Initialise the decryption cipher. */
		this.decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
	}
	
	/**
	 * Constructor to read our public and private keys from a file.
	 * 
	 * @param publicKeyFileName The path to the file containing our public key.
	 * @param privateKeyFileName The path to the file containing our private 
	 * key.
	 * @param password The password to decrypt the private key file.
	 * @param peer The public key of the the peer of the communications, used 
	 * for encryption. If null, then encryption will be unavailable.
	 * 
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidKeySpecException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidPasswordException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	public RSAAsymmetricEncryption(String publicKeyFileName, String privateKeyFileName, String password, PublicKey peer) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidPasswordException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		/** Establish the keys. */
		this.publicKey = readPublicKeyFromFile(publicKeyFileName);
		this.privateKey = readPrivateKeyFromFile(privateKeyFileName, password);
		
		/** Initialise the encryption cipher. */
		setPeerPublicKey(peer);
		
		/** Initialise the decryption cipher. */
		this.decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
	}
	
	/**
	 * Constructor to read our public and private keys from a file.
	 * 
	 * @param publicKeyFile The file containing our public key.
	 * @param privateKeyFile The file containing our private key. 
	 * @param password The password to decrypt the private key file.
	 * @param peer The public key of the the peer of the communications, used 
	 * for encryption. If null, then encryption will be unavailable.
	 * 
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidKeySpecException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidPasswordException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	public RSAAsymmetricEncryption(URL publicKeyFile, URL privateKeyFile, String password, PublicKey peer) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidPasswordException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		/** Establish the keys. */
		this.publicKey = readPublicKeyFromFile(publicKeyFile);
		this.privateKey = readPrivateKeyFromFile(privateKeyFile, password);
		
		/** Initialise the encryption cipher. */
		setPeerPublicKey(peer);
		
		/** Initialise the decryption cipher. */
		this.decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
	}
	
	/**
	 * Constructor to use the supplied public/private key pair.
	 * 
	 * @param publicKey Our public key.
	 * @param privateKey Our private key. 
	 * @param peer The public key of the the peer of the communications, used 
	 * for encryption. If null, then encryption will be unavailable.
	 * 
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchProviderException 
	 */
	public RSAAsymmetricEncryption(PublicKey publicKey, PrivateKey privateKey, PublicKey peer) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException {
		/** Establish the keys. */
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		
		/** Initialise the encryption cipher. */
		setPeerPublicKey(peer);
		
		/** Initialise the decryption cipher. */
		this.decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
	}
	
	/**
	 * Constructor to use the supplied asymmetric encryption provider. The 
	 * supplied asymmetric encryption provider will be cloned except that the 
	 * specified peer public key will be used.
	 * 
	 * @param ae An AsymmetricEncryption instance.
	 * @param peer The public key of the the peer of the communications, used 
	 * for encryption. If null, then encryption will be unavailable.
	 * 
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchProviderException 
	 */
	public RSAAsymmetricEncryption(AsymmetricEncryption ae, PublicKey peer) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException {
		/** Establish the keys. */
		this.publicKey = ae.getPublicKey();
		this.privateKey = ae.getPrivateKey();
		
		/** Initialise the encryption cipher. */
		setPeerPublicKey(peer);
		
		/** Initialise the decryption cipher. */
		this.decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
	}
	
	/**
	 * Encrypts a message using the peer public key. If the cleartext is longer
	 * than the maximum allowable cleartext (MAX_CLEARTEXT), then the cleartext 
	 * will be encrypted in chunks of length MAX_CLEARTEXT.
	 * 
	 * @param cleartext The message to be encrypted.
	 * @return The ciphertext message.
	 * 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws IllegalStateException
	 */
	public byte[] encrypt(String cleartext) throws IllegalBlockSizeException, BadPaddingException, IllegalStateException {
		return encrypt(cleartext.getBytes());
	}
	
	/**
	 * Encrypts a message using the peer public key. If the cleartext is longer
	 * than the maximum allowable cleartext (MAX_CLEARTEXT), then the cleartext
	 * will be encrypted in chunks of length MAX_CLEARTEXT.
	 * 
	 * @param cleartext The message to be encrypted.
	 * @return The ciphertext message.
	 * 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws IllegalStateException
	 */
	public byte[] encrypt(byte[] cleartext) throws IllegalBlockSizeException, BadPaddingException, IllegalStateException {
		if (encryptionCipher == null)
			throw new IllegalStateException("Cannot perform encryption without a peer public key.");
		
		/** TODO: tidy this code. */
		
		/** 
		 * Split the cleartext up into chunks and encrypt each chunk separately.
		 */
    	final Queue<byte[]> chunks = new LinkedList<byte[]>();
    	int startIndex = 0;
    	int totalLength = 0;
    	
    	final int chunkSize = MAX_CLEARTEXT;
    	final int chunkCount = (int) Math.ceil((double) cleartext.length / (double) chunkSize);
    	
    	for (int i = 0; i < chunkCount; i++) {
    		/** Get the size of the chunk. */
    		byte[] chunk;
    		if (startIndex + chunkSize > cleartext.length)
    			chunk = new byte[cleartext.length - startIndex];
    		else
    			chunk = new byte[chunkSize];
    		
    		/** Copy the unencrypted chunk. */
    		System.arraycopy(cleartext, startIndex, chunk, 0, chunk.length);
    		startIndex += chunk.length;

    		/** Encrypt this chunk and add it to the queue. */
    		final byte[] encryptedChunk = encryptionCipher.doFinal(chunk);
    		System.out.println(chunkSize + " => " + encryptedChunk.length);
    		chunks.add(encryptedChunk);
    		totalLength += encryptedChunk.length;
    	}
    	
    	/** Combine the encrypted chunks. */
    	int currentIndex = 0;
    	final byte[] combinedChunks = new byte[totalLength];
    	while (chunks.size() > 0) {
    		final byte[] chunk = chunks.remove();
    		System.arraycopy(chunk, 0, combinedChunks, currentIndex, chunk.length);
    		currentIndex += chunk.length;
        }
		
    	/** Encode the combined encrypted chunks. */
        final byte[] encodedValue = Base64.encodeBase64(combinedChunks);
        return encodedValue;
	}
	
	/**
	 * Decrypts a message using our private key.
	 * 
	 * @param ciphertext The message to be decrypted.
	 * @return The cleartext message.
	 * 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public byte[] decrypt(String ciphertext) throws IllegalBlockSizeException, BadPaddingException {
		return decrypt(ciphertext.getBytes());
	}
	
	/**
	 * Decrypts a message using our private key.
	 * 
	 * @param ciphertext The message to be decrypted.
	 * @return The cleartext message.
	 * 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public byte[] decrypt(byte[] ciphertext) throws IllegalBlockSizeException, BadPaddingException {
		/** Decode the combined encrypted chunks. */
		final byte[] decodedValue = Base64.decodeBase64(ciphertext);
		
		/** TODO: tidy this code. */
		
		/** 
		 * Split the ciphertext up into chunks and decrypt each chunk separately.
		 */
    	final Queue<byte[]> chunks = new LinkedList<byte[]>();
    	int startIndex = 0;
    	int totalLength = 0;
    	
    	final int chunkSize = MAX_CIPHERTEXT;
    	final int chunkCount = (int) Math.ceil((double) decodedValue.length / (double) chunkSize);
    	
    	for (int i = 0; i < chunkCount; i++) {
    		/** Get the size of the chunk. */
    		byte[] chunk;
    		if (startIndex + chunkSize > decodedValue.length)
    			chunk = new byte[decodedValue.length - startIndex];
    		else
    			chunk = new byte[chunkSize];
    		
    		/** Copy the encrypted chunk. */
    		System.arraycopy(decodedValue, startIndex, chunk, 0, chunk.length);
    		startIndex += chunk.length;

    		/** Decrypt this chunk and add it to the queue. */
    		System.out.println(i + ", " + chunk.length);
    		final byte[] decryptedChunk = decryptionCipher.doFinal(chunk);
    		chunks.add(decryptedChunk);
    		totalLength += decryptedChunk.length;
    	}
    	
    	/** Combine the decrypted chunks. */
    	int currentIndex = 0;
    	final byte[] combinedChunks = new byte[totalLength];
    	while (chunks.size() > 0) {
    		final byte[] chunk = chunks.remove();
    		System.arraycopy(chunk, 0, combinedChunks, currentIndex, chunk.length);
    		currentIndex += chunk.length;
        }
    	
        return combinedChunks;
	}
	
	/**
	 * Get our public key.
	 * 
	 * @return Our public key.
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
	
	/**
	 * Get the peer's public key. The peer's public key is used for encryption.
	 * 
	 * @return The peer's public key.
	 */
	public PublicKey getPeerPublicKey() {
		return peerPublicKey;
	}
	
	/**
	 * Set the peer's public key. The peer's public key is used for encryption.
	 * 
	 * @param peer The peer's public key.
	 * 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchProviderException 
	 */
	public void setPeerPublicKey(PublicKey peer) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, NoSuchProviderException {
		this.peerPublicKey = peer;
		
		if (this.peerPublicKey != null) {
			this.encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			this.encryptionCipher.init(Cipher.ENCRYPT_MODE, this.peerPublicKey);
		} else {
			this.encryptionCipher = null;
		}
	}
	
	/**
	 * Get our private key. Our private key is used for decryption.
	 * 
	 * @return Our private key.
	 */
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
	
	/**
	 * Save the public key to a file so that it can be retrieved at a later 
	 * time. The public key file will not be encrypted in any way.
	 * 
	 * @param filename The path of the file to which the public key should be 
	 * saved.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws IOException
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	public void savePublicKeyToFile(String filename) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException {
		final KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
		final RSAPublicKeySpec keySpec = factory.getKeySpec(this.publicKey, RSAPublicKeySpec.class);
		writeToFile(filename, keySpec.getModulus(), keySpec.getPublicExponent(), null);
	}
	
	/**
	 * Save the private key to a file so that it can be retrieved at a later 
	 * time. The private key file will be encrypted using a user-supplied 
	 * password.
	 * 
	 * @param filename The path of the file to which the public key should be 
	 * saved.
	 * @param password The password to encrypt the file.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws IOException
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	public void savePrivateKeyToFile(String filename, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		final KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
		final RSAPrivateKeySpec keySpec = factory.getKeySpec(this.privateKey, RSAPrivateKeySpec.class);
		writeToFile(filename, keySpec.getModulus(), keySpec.getPrivateExponent(), password);
	}
	
	/**
	 * A utility function to write the modulus and exponent of a key to a file
	 * encrypted with a password.
	 * 
	 * @param filename The path of the file to write to.
	 * @param mod The modulus of the key.
	 * @param exp The exponent of the key.
	 * @param password A password to encrypt the file. Null for no password
	 * 
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeySpecException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private static void writeToFile(String filename, BigInteger mod, BigInteger exp, String password) throws IOException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchPaddingException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
		final DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
		
		/** Write the modulus and exponent to a byte array. */
		try {
			/** Write the modulus to the output byte array. */
			final byte[] modArray = mod.toByteArray();
			dataOutputStream.writeInt(modArray.length);
			dataOutputStream.write(modArray);
			
			/** Write the exponent to a byte array. */
			final byte[] expArray = exp.toByteArray();
			dataOutputStream.writeInt(expArray.length);
			dataOutputStream.write(expArray);
		} catch (Exception e) {
			throw new IOException("Unexpected error", e);
		} finally {
			dataOutputStream.flush();
			bufferedOutputStream.flush();
			outputStream.flush();
			dataOutputStream.close();
			bufferedOutputStream.close();
		}
		
		/** Write the byte array to an (un)encrypted file. */
		if (password != null) {
			EncryptedFile file = new EncryptedFile(outputStream.toByteArray(), password);
			file.writeToFile(new File(filename));
		} else {
			final FileOutputStream fileOutputStream = new FileOutputStream(filename);
			
			try {
				fileOutputStream.write(outputStream.toByteArray());
			} catch (Exception e) {
				throw new IOException("Unexpected error", e);
			} finally {
				fileOutputStream.flush();
				fileOutputStream.close();
			}
		}
		
		/** Close the output stream. */
		outputStream.close();
	}
	
	/**
	 * Read a public key from a file.
	 * 
	 * @param filename The path to the file containing the public key.
	 * @return The public key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchProviderException 
	 */
	public static PublicKey readPublicKeyFromFile(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		return readPublicKeyFromFile(new FileInputStream(filename));
	}
	
	/**
	 * Read a public key from a file.
	 * 
	 * @param file The file containing the public key.
	 * @return The public key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchProviderException 
	 */
	public static PublicKey readPublicKeyFromFile(URL file) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		return readPublicKeyFromFile(file.openStream());
	}
	
	/**
	 * Read the public key from a file.
	 * 
	 * @param fileInputStream The input stream for the file containing the 
	 * public key.
	 * @return The public key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchProviderException 
	 */
	public static PublicKey readPublicKeyFromFile(InputStream inputStream) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		final DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
		
		/** Get the modulus and exponent from the decrypted data. */
		BigInteger mod = null;
		BigInteger exp = null;
		try {
			final int modBytes = dataInputStream.readInt();
			final byte[] modByteArray = new byte[modBytes];
			dataInputStream.read(modByteArray);
			mod = new BigInteger(modByteArray);
			
			final int expBytes = dataInputStream.readInt();
			final byte[] expByteArray = new byte[expBytes];
			dataInputStream.read(expByteArray);
			exp = new BigInteger(expByteArray);
		} catch (Exception e) {
			throw new IOException("Unexpected error", e);
		} finally {
			dataInputStream.close();
			bufferedInputStream.close();
		}
		
		/** Recreate the public key. */
		final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, exp);
		final KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
	    final PublicKey pubKey = keyFactory.generatePublic(keySpec);
		
		return pubKey;
	}
	
	/**
	 * Read a private key from a password-encrypted file.
	 * 
	 * @param filename The path to the file containing the private key.
	 * @param password The password to decrypt the private key file.
	 * @return The private key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidPasswordException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	private static PrivateKey readPrivateKeyFromFile(String filename, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidPasswordException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		return readPrivateKeyFromFile(new EncryptedFile(new File(filename), password));
	}
	
	/**
	 * Read a private key from a password-encrypted file.
	 * 
	 * @param file The file containing the private key.
	 * @param password The password to decrypt the file.
	 * @return The private key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidPasswordException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	private static PrivateKey readPrivateKeyFromFile(URL file, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidPasswordException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		return readPrivateKeyFromFile(new EncryptedFile(file, password));
	}
	
	/**
	 * Read a private key from a password-encrypted file.
	 * 
	 * @param file The file containing the private key.
	 * @return The private key contained within the file.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidPasswordException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchProviderException 
	 */
	private static PrivateKey readPrivateKeyFromFile(EncryptedFile file) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidPasswordException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		final byte[] decryptedData = file.decrypt();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedData);
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		final DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
		
		/** Get the modulus and exponent from the decrypted data. */
		BigInteger mod = null;
		BigInteger exp = null;
		try {
			final int modBytes = dataInputStream.readInt();
			final byte[] modByteArray = new byte[modBytes];
			dataInputStream.read(modByteArray);
			mod = new BigInteger(modByteArray);
					
			final int expBytes = dataInputStream.readInt();
			final byte[] expByteArray = new byte[expBytes];
			dataInputStream.read(expByteArray);
			exp = new BigInteger(expByteArray);
		} catch (Exception e) {
			throw new IOException("Unexpected error", e);
		} finally {
			dataInputStream.close();
			bufferedInputStream.close();
			inputStream.close();
		}
		
		/** Recreate the private key. */
		final RSAPrivateKeySpec rsaKeySpec = new RSAPrivateKeySpec(mod, exp);
		final KeyFactory rsaKeyFactory = KeyFactory.getInstance(ALGORITHM);
	    final PrivateKey privKey = rsaKeyFactory.generatePrivate(rsaKeySpec);
		
		return privKey;
	}
}

/******************************************************************************
 * END OF FILE:     RSAAsymmetricEncryption.java
 *****************************************************************************/