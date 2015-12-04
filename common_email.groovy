@Grab(group='javax.mail', module='javax.mail-api', version='1.5.4')
@Grab(group='com.sun.mail', module='mailapi', version='1.5.4')
@Grab(group='com.sun.mail', module='smtp', version='1.5.4')

import javax.mail.*
import javax.mail.internet.*

class common_email {
    def static host = "smtp.gmail.com"

	def static simpleMail(String from, String password, String to,
						  String subject, String body)
							throws Exception {
 
    	Properties props = System.getProperties()
    	props.put("mail.smtp.starttls.enable",true)
    	/* mail.smtp.ssl.trust is needed in script to avoid error "Could not convert socket to TLS"  */
    	props.setProperty("mail.smtp.ssl.trust", host)
   	 	props.put("mail.smtp.auth", true)
    	props.put("mail.smtp.host", host)
    	props.put("mail.smtp.user", from)
    	props.put("mail.smtp.password", password)
    	props.put("mail.smtp.port", "587")
 
    	Session session = Session.getDefaultInstance(props, null)
   		MimeMessage message = new MimeMessage(session)
    	message.setFrom(new InternetAddress(from))
 
    	InternetAddress toAddress = new InternetAddress(to)
 
    	message.addRecipient(Message.RecipientType.TO, toAddress)
 
    	message.setSubject(subject)
    	message.setText(body)
 
    	Transport transport = session.getTransport("smtp")
 
    	transport.connect(host, from, password)
 
    	transport.sendMessage(message, message.getAllRecipients())
    	transport.close()
	}

}
 

