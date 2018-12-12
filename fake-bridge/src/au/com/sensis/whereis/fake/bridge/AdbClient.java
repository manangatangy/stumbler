package au.com.sensis.whereis.fake.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The AdbClient manages the communications with the adb-connected remote device/emulator.
 * The ctor starts a new thread in which all the adb/socket comms takes place.
 * Clients communicate with this thread via two queues (one for reading and one for writing).
 * 
 * The ctor starts a worker thread that tries every 1000 millis to establish a connection to the
 * specified localhost port.  Once connected, the thread sends lines from the write queue to the
 * socket and receives lines from the socket and places them into the read queue.  The queues
 * may be accessed from the client via read() and write() methods.
 * You should probably check the isConnected() status before reading or writing (although these
 * methods won't block). 
 * If the connection is lost or there is some exception other than caused by interrupt, then the
 * thread will reset and resume connection attempts.  
 * Call terminate to interrupt the worker thread and cease using this instance.
 */
public class AdbClient implements Runnable {

	private int adbPort;
	private String adbCommand;
	private int fakePort;						// Used only for diagnostics.
	
	private SocketChannel socketChannel;		// Operated in non-blocking mode.
	private Thread thread;
	
	private BlockingQueue<String> readQueue = new ArrayBlockingQueue<String>(1000);
	private BlockingQueue<String> writeQueue = new ArrayBlockingQueue<String>(1000);
	
	/** 
	 * Establish a socket channel with the specified adbPort.  If the adbCommand is non-null, then
	 * prior to opening the socket, this command is executed (to setup the required port forwarding).
	 * 
	 * @param adbPort
	 */
	public AdbClient(int adbPort, String adbCommand, int fakePort) {
		this.adbPort = adbPort;
		this.adbCommand = adbCommand;
		this.fakePort = fakePort;
		this.thread = new Thread(this, "AdbClient");
		this.thread.start();
	}

	/**
	 * This instance can no longer be used.
	 */
	public void terminate() {
		thread.interrupt();
		thread = null;
	}
	
	public boolean isConnected() {
		return socketChannel != null && socketChannel.isConnected();
	}

//	/* Periodically, a timer thread will trigger a ping to the device
//	 * by setting pingPending.   The socket thread periodically checks 
//	 * this flag, and when it's set (and the write queue is 
//	 * When this occurs, the socket thread will check if the writeQueue
//	 * is empty and if so, will initiate the ping process.  During this
//	 * process the external calls to read() and write() will block, and
//	 * the queues will be used exclusively for pinging, within readWrite().
//	 */
//	private boolean pingPending = false;
//	private boolean pingInProgress = false;
//
//	public String sendAndWaitForReply(String line, long timeoutInMillis) throws InterruptedException {
//		if (pingInProgress) {
//	        synchronized (this) {
//	            wait(0);			// Until the ping has completed.
//	        }
//	        pingInProgress = false;
//		}
//		
//		return sendAndWait(line, timeoutInMillis);
//	}
	
	/* Periodically, a timer thread will trigger a ping to the device.
	 * When this occurs, the socket thread will check if the writeQueue
	 * is empty and if so, will initiate the ping process.  During this
	 * process the external calls to read() and write() will block, and
	 * the queues will be used exclusively for pinging, within readWrite().
	 * 
        synchronized (this) {
            wait(UPDATE_LOCATION_WAIT_TIME);
        }
                if (observer != null) {
                    synchronized (observer) {
                        observer.notify();
                    }
                }

	 * 
	 */
	
	/**
	 * First empties the read queue and then sends a line and returns the 
	 * response, waiting up to timeoutInMillis for a reply else returns null.   
	 */
	public String sendAndWaitForReply(String line, long timeoutInMillis) {
		readQueue.clear();
		write(line);
		return read(timeoutInMillis);
	}
	
	/**
	 * Waits for timeoutInMillis or until a line is available, which is returned (else null if times out).
	 */
	private String read(long timeoutInMillis) {
		String line = null;
		try {
			line = readQueue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException iexcIgnored) {
		}
		return line;
	}
	
	/**
	 * Blocking write. 
	 */
	private void write(String line) {
		try {
			writeQueue.put(line);
		} catch (InterruptedException iexcIgnored) {
		}
	}
	

	private void log(String message) {
        System.out.println("AdbClient: " + message);
	}
	
	@Override
	public void run() {

		log("starting");
		
		try {
			// The main loop runs until interrupted by an InterruptException.
			// There are two inner loops nested in an outer loop.  The first inner loop attempts to
			// connect to the server, sleeping briefly between attempts.  The second inner loop 
			// reads/writes between the queues and the socket, and is breaked should an IOException 
			// occur, at which point the first inner loop resumes connection attempts.  
			while (true) {
				while (!connect()) {
		        	Thread.sleep(1000);			// Wait a bit and try again.
				}
				while (readWrite()) {
					// Continuously read/write until an IOException.
					//Thread.sleep(1000);
				}
			}
		} catch (InterruptedException iexc) {
			log("interrupted by: " + iexc.getMessage());
		}
		
		if (socketChannel != null) {
			// Close quietly
			try {
				socketChannel.close();
			} catch (IOException ioeIgnored) {
			}
		}
		
		log("exiting");
	}
	
	private boolean currentlyConnected = false;
	
	/**
	 * This method assumes a new SocketChannel must be created and (re)connected.
	 * Execute the adbCommand (if specified), then create and open the socket channel.
	 * If either fail, catch only IOExceptions and return false. Else returns true
	 * and the socketchannel may be io-ed.
	 * Track the connection status so we can advise whenever there is a change. 
	 * @throws InterruptedException 
	 */
	private boolean connect() throws InterruptedException {
		if (currentlyConnected) {
			log("disconnected from port: " + adbPort);    // We must have been disconnected, after a previous successful connection.
			currentlyConnected = false;
		}
		try {
			if (adbCommand != null) {
				Process process = Runtime.getRuntime().exec(adbCommand);
				int returnCode = process.waitFor();
				log("executed: " + adbCommand + ", ==> " + returnCode);
			}
	        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", adbPort));
	        log("new socket channel connected at port: " + adbPort + (adbCommand == null ? "" : (" forwarded to " + fakePort)));
	        log("isBlocking ==> " + socketChannel.isBlocking());
	        socketChannel.configureBlocking(false);
	        log("isBlocking ==> " + socketChannel.isBlocking());
			currentlyConnected = true;
		} catch (IOException ioeIgnored) {
		}
		return currentlyConnected;
	}

	/**
	 * Perform a read/write cycle, transferring data between the queues 
	 * and the socket channel.  if an IOException occurs, then return false. 
	 * Otherwise returns true.  Interrupts are not caught.
	 */
	private boolean readWrite() throws InterruptedException {
		try {
			// Setup the read buffers.
	    	Charset charset = Charset.forName("ISO-8859-1");
	    	CharsetDecoder decoder = charset.newDecoder();
	    	CharsetEncoder encoder = charset.newEncoder();
	    	
	    	ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	        CharBuffer charBuffer = CharBuffer.allocate(1024);
	        LineAssembler assembler = new LineAssembler();
	        
	        if ((socketChannel.read(buffer)) > 0) {		// Non-blocking read. May be interrupted.
	        	buffer.flip();
	        	decoder.decode(buffer, charBuffer, false);
	        	charBuffer.flip();
	        	String fragment = charBuffer.toString();
	        	
	            log("received fragment '" + fragment + "'");
	        	if (assembler.isCompleteLineAfterAppending(fragment)) {
	        		String line = assembler.nextLine();
	                log("received complete line: " + line);
	    			readQueue.put(line);
	        	}
	        	buffer.clear();
	        	charBuffer.clear();
	        }
	        
	        // Now try and write something.
	        if (writeQueue.peek() != null) {
	        	String line = writeQueue.take();
	            log("sending line: " + line);
    			if (!line.endsWith("\n"))		// Needs a line terminator for the reader on the other end.
    				line = line + "\n";
	            socketChannel.write(encoder.encode(CharBuffer.wrap(line)));
	            
//	        	charBuffer = CharBuffer.wrap(line);
//	        	encoder.encode(charBuffer, buffer, false);
//	        	socketChannel.write(buffer);
//	        	buffer.clear();
//	        	charBuffer.clear();
	        }
	        return true;
		} catch (IOException ioeIgnored) {
	        return false;
		}
	}
	
}
