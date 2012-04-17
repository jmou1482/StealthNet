/******************************************************************************
 * ELEC5616
 * Computer and Network Security, The University of Sydney
 * Copyright (C) 2002-2004, Matt Barrie and Stephen Gould
 *
 * PACKAGE:         StealthNet
 * FILENAME:        FileTransfer.java
 * AUTHORS:         Matt Barrie and Stephen Gould
 * DESCRIPTION:     Implementation of StealthNet Client FTP for ELEC5616
 *                  programming assignment.
 *                  This code has been written for the purposes of teaching
 *                  cryptography and computer security. It is to be used as
 *                  a demonstration only. No attempt has been made to optimise
 *                  the source code.
 * VERSION:         1.0-ICE
 *
 * REVISION HISTORY:
 *
 *****************************************************************************/

package StealthNet;

/* Import Libraries **********************************************************/

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JOptionPane;

/* FileTransfer Class Definition *********************************************/

/**
 * A class to manage a file transfer between multiple StealthNet clients.
 * 
 * @author Matt Barrie
 * @author Stephen Gould
 * @author Joshua Spence (Added debug code.)
 */
public class FileTransfer extends Thread {
	/** Debug options. */
	private static final boolean DEBUG_GENERAL     = Debug.isDebug("StealthNet.FileTransfer.General");
	private static final boolean DEBUG_ERROR_TRACE = Debug.isDebug("StealthNet.FileTransfer.ErrorTrace") || Debug.isDebug("ErrorTrace");
	private static final boolean DEBUG_TRANSFER    = Debug.isDebug("StealthNet.FileTransfer.Transfer");
	
	/** Number of bytes to send at a time. */
    private static final int PACKETSIZE = 256;

    /** A progress bar to visualise the transfer. */
    private JProgressBar progressBar = null;
    
    /** The communications class through which to perform the transfer. */
    private Comms stealthComms = null;
    
    /** The filename of the file being transferred. */
    private String filename;
    
    /** True to indicate sending, false to indicate receiving. */
    private boolean bSend;

    /** 
     * Constructor. 
     * 
     * @param snComms The StealthNet.Comms instance to use for the transfer.
     * @param fn The filename of the file to be transferred.
     * @param send True to indicate sending, false to indicate receiving.
     */
    public FileTransfer(Comms snComms, String fn, boolean send) {
        stealthComms = snComms;
        filename = fn.trim();
        bSend = send;
    }

    /** 
     * Initialise the GUI components for the file transfer. 
     * 
     * @return An AWT component containing all GUI elements for the file 
     * transfer.
     */
    public Component createGUI() {
        /** Create progress bar. */
        progressBar = new JProgressBar(0, 10);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        /** Create top-level panel and add components. */
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.setLayout(new BorderLayout());
        pane.add(progressBar, BorderLayout.NORTH);

        return pane;
    }

    /** Transfer the file. */
    public void run() {
    	/** Get the screen size. */
        final Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

        /** Set up FTP window */
        final JFrame ftpFrame = new JFrame("stealthnet FTP [" + filename + "]");
        ftpFrame.getContentPane().add(createGUI(), BorderLayout.CENTER);
        ftpFrame.pack();

        /** Centre the window. */
        final int x = (screenDim.width - ftpFrame.getSize().width) / 2;
        final int y = (screenDim.height - ftpFrame.getSize().height) / 2;
        ftpFrame.setLocation(x, y);
        ftpFrame.setVisible(true);

        /** Send or receive the file. */
        if (bSend) sendFile();
        else       recvFile();

        /** Upload/Download complete. */
        if (DEBUG_GENERAL) System.out.println((bSend ? "Upload" : "Download") + " complete.");
        
        ftpFrame.setVisible(false);
        JOptionPane.showMessageDialog(ftpFrame,
            (bSend ? "Upload Complete" : "Download Complete"),
            "StealthNet", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Send the file. */
    private synchronized void sendFile() {
        byte[] buf = new byte[PACKETSIZE];
        final int fileLen = (int) ((new File(filename)).length() / PACKETSIZE);

        if (DEBUG_GENERAL) System.out.println("Sending file \"" + filename + "\" of size " + fileLen + ".");
        
        progressBar.setMaximum(fileLen);
        try {
        	/** Setup the transfer. */
        	if (DEBUG_GENERAL) System.out.println("Setting up file transfer.");
            stealthComms.sendPacket(Packet.CMD_FTP, Integer.toString(fileLen));
            
            /** Receive server response. */
            if (DEBUG_GENERAL) System.out.println("Waiting for server response.");
            stealthComms.recvPacket();
            
            final FileInputStream fid = new FileInputStream(filename);
            int bufLen;
            do {
                bufLen = fid.read(buf);
                if (bufLen > 0) {
                	/** Send a part of the file. */
                	if (DEBUG_TRANSFER) System.out.println("Sending " + bufLen + " bytes of \"" + filename + "\".");
                    stealthComms.sendPacket(Packet.CMD_FTP, buf, bufLen);
                    
                    /** Wait for server response. */
                    if (DEBUG_TRANSFER) System.out.println("Waiting for server response.");
                    stealthComms.recvPacket();
                }
                
                /** Update the progress bar. */
                progressBar.setValue(progressBar.getValue() + 1);
            } while (bufLen > 0);
            
            /** Close file handle. */
            fid.close();
            if (DEBUG_GENERAL) System.out.println("Sending terminating file transfer packet.");
            stealthComms.sendPacket(Packet.CMD_FTP);
        } catch (IOException e) {
            System.err.println("Error reading from file \"" + filename + "\".");
            if (DEBUG_ERROR_TRACE) e.printStackTrace();
        }
    }

    /**  Receive the file. */
    private synchronized void recvFile() {
        try {
        	/** Get the file length from the first packet. */
        	if (DEBUG_GENERAL) System.out.println("Waiting for sender to transmit file length.");
            final int fileLen = (new Integer(new String(stealthComms.recvPacket().data))).intValue();
            if (DEBUG_GENERAL) System.out.println("Expecting to receive file \"" + filename + "\" of size " + fileLen + " bytes.");
            
            /** Acknowledge with a NULL packet. */
            if (DEBUG_GENERAL) System.out.println("Sending acknowledgement to sender.");
            stealthComms.sendPacket(Packet.CMD_NULL);
            
            /** Set the scale on the progress bar. */
            progressBar.setMaximum(fileLen);
            
            /** Create an output stream for the received file. */
            final FileOutputStream fid = new FileOutputStream(filename);
            
            /** 
             * Keep receiving file data, sending an acknowledgement (NULL 
             * packet) each time. The file data is written to the file output 
             * stream.
             */
            byte[] buf;
            do {
            	/** Receive file data. */
                buf = stealthComms.recvPacket().data;
                if (DEBUG_TRANSFER) System.out.println("Received " + buf.length + " bytes of file.");
                
                /** Send an acknowledgement (NULL packet). */
                if (DEBUG_TRANSFER) System.out.println("Sending acknowledgement.");
                stealthComms.sendPacket(Packet.CMD_NULL);
                
                /** Write the file data to the file output stream. */
                fid.write(buf);
                
                /** Update the progress bar to represent the current progress. */
                progressBar.setValue(progressBar.getValue() + 1);
            } while (buf.length > 0);
            
            /** Close file handle. */
            fid.close();
        } catch (IOException e) {
            System.err.println("Error writing to file \"" + filename + "\".");
            if (DEBUG_ERROR_TRACE) e.printStackTrace();
        }
   }
}

/******************************************************************************
 * END OF FILE:     FileTransfer.java
 *****************************************************************************/