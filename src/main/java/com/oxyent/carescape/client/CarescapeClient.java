package com.oxyent.carescape.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Configuration
@ComponentScan( {"com.oxyent.carescape.client"} )
@PropertySource( { "classpath:application.properties" } )
public class CarescapeClient implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CarescapeClient.class);

    private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	
	@Autowired
	private Environment env;

	public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(CarescapeClient.class, args);
		//new CarescapeClient().run(args);
	}
	
    public void run(String... args) throws Exception {
		System.out.println("Inside run ");
        String host = env.getProperty("carescape.host");
		int port = Integer.parseInt(env.getProperty("carescape.port"));
		try {
			boolean connectionOpened = initSocketAlongWithStreams(host, port);
			if (connectionOpened) {
				Message helloMessage = createMessage();
				writeMessageToOutStream((MimeMessage) helloMessage);
				MimeMessage helloMessageReply = readMessageFromInStream();
				logger.info("helloMessageReply from carescape server {} ", helloMessageReply.getContent().toString());
			} else {
				closeSocket();
			}
		} catch (Exception e) {
			logger.error("CarescapeClientMain: some exception occured ", e);
		} finally {
			closeStreams();
			closeSocket();
		}
		logger.info("Shuting down the application...");
	}
	
	public void closeStreams() {
		logger.info("Closing in and our streams...");
		try {
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (IOException e) {
			logger.error("Exception while closing the in and out streams ", e);
		}
	}

	private void closeSocket() {
		logger.info("Closing socket...");
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			logger.error("Exception while closing the socket ", e);
		}
	}

	public boolean initSocketAlongWithStreams(String host, int port) throws IOException {
		try {
			logger.info("Opening socket along with in and out stream to host {} and port {}", host, port);
			socket = new Socket(host, port);
			outputStream = socket.getOutputStream();
			inputStream = socket.getInputStream();
			if (outputStream != null && inputStream != null) {
				return true;
			}
		} catch (UnknownHostException e) {
			logger.error("Exception while opening socket/in or out stream ", e);
			e.printStackTrace();
			throw e;
		}
		return false;
	}
	
	public void writeMessageToOutStream(MimeMessage message) {
		try {
			logger.info("Writing message to out stream...");
			message.writeTo(outputStream);
		} catch (IOException e) {
			logger.error("IOException while writing message to out stream ", e);
		} catch (MessagingException e) {
			logger.error("MessagingException writing message to out stream ", e);
		}
	}
	
	public MimeMessage readMessageFromInStream() throws IOException, MessagingException {
		try {
			logger.info("Reading message from in stream...");
			StringBuilder sb = new StringBuilder();
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		    while ((line = br.readLine()) != null) {
		      System.out.println(line);
		      sb.append(line);
		    }

		    br.close();
			ByteArrayInputStream is = new ByteArrayInputStream(sb.toString().getBytes());
			MimeMessage message = new MimeMessage((Session)null, is);
			return message;
		} catch (IOException e) {
			logger.error("IOException while reading message from in stream ", e);
			throw e;
		} catch (MessagingException e) {
			logger.error("MessagingException reading message from in stream ", e);
			throw e;
		}
	}

	public Message createMessage() {
		// Create a default MimeMessage object.
		Message message = new MimeMessage((Session) null);
		try {

			// Create a multipart message
			Multipart multipart = new MimeMultipart();

			BodyPart messageBodyPart = new MimeBodyPart();
			long length = FileUtils.getFile("/Users/dharmender/Documents/workspace/carescape-client/src/main/resources/hello.xml").length();
			//String helloMsgXml = FileUtils.readFileToString(new File("/Users/dharmender/Documents/workspace/carescape-client/src/main/resources/hello.xml"), "UTF-8");
			//messageBodyPart.setText(helloMsgXml);
			String filename = "/Users/dharmender/Documents/workspace/carescape-client/src/main/resources/hello.xml";
			DataSource source = new FileDataSource(filename);
			messageBodyPart.setDataHandler(new DataHandler(source));
			
			messageBodyPart.setHeader("Content-Length", String.valueOf(length));
			messageBodyPart.setHeader("Content-Type", "                                                    application/x-sapphire+xml");
			messageBodyPart.setHeader("Content-Transfer-Encoding", "binary");
			multipart.addBodyPart(messageBodyPart);
			
			// Send the complete message parts
			message.setContent(multipart);
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			message.writeTo(bais);
			message.setHeader("Content-Length", String.valueOf(bais.size()));

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}

}
