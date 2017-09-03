package net.vargadaniel.re.reportuploader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ReportEngine.class)
public class ReportUploader {
	
	static Logger log = LoggerFactory.getLogger(ReportUploader.class);
	
	@StreamListener(ReportEngine.REPORT_FILES)
	@SendTo(ReportEngine.STATUS_UPDATES)
	public StatusUpdate processReportFile(Message<String> reportMsg) {
		String content = reportMsg.getPayload();
		Long orderId = (Long) reportMsg.getHeaders().get("orderId");
		String productName = (String) reportMsg.getHeaders().get("productName");
		String fileName = productName + "_" + orderId + ".xml";
		String server = "35.193.121.213";
		String username = "";
		String password = "";
		if (uploadReport(content.getBytes(), fileName, server, username, password)) {
			return new StatusUpdate(orderId, "report uploaded successfully");
		} else {
			return new StatusUpdate(orderId, "report upload failed");
		}
	}
	
	public static boolean uploadReport(byte[] content, String fileName, String server, String username, String password) {
	    FTPClient ftp = new FTPClient();
	    ftp.setConnectTimeout(10000);
	    ftp.setDataTimeout(10000);
	    try {
	      ftp.connect(server);
	      log.info("Connected to: {}; reply: {} ", server, ftp.getReplyString());
	      // After connection attempt, you should check the reply code to verify
	      // success.
	      if(!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
	        ftp.disconnect();
	        log.error("FTP server refused connection.");
	      }
	      
	      log.info("About to login to {} as {}", server, username);
	      if (!ftp.login(username, password)) {
	    	  	log.error("Cloud FTP login to {} with user {}", server, username);
	    	  	return false;
	      }
	      log.info("Logged in to {} as {}", server, username);
	      
	      InputStream in = new ByteArrayInputStream(content);
	      ftp.enterLocalPassiveMode();
	      log.info("About to upload {}", fileName);
	      ftp.storeUniqueFile(fileName, in);
	      log.info("{} uploaded.", fileName);
	
	      ftp.logout();
	      return true;
	    } catch(IOException e) {
	      log.error("FTP error while uploading files", e);
	      return false;
	    } finally {
	      if(ftp.isConnected()) {
	        try {
	          ftp.disconnect();
	        } catch(IOException ioe) {
	          log.error("Error while ftp.disconnect from {}", server);
	        }
	      }
	    }
	}
	
	static class StatusUpdate {
		
		public Long getOrderId() {
			return orderId;
		}

		public String getStatus() {
			return status;
		}

		public StatusUpdate(Long orderId, String status) {
			super();
			this.orderId = orderId;
			this.status = status;
		}

		final Long orderId;
		
		final String status;
		
	}
}


