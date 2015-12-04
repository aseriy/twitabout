import static common_email.*


def emailTest() {
	/* Set email address sender */
	def s1 = "shiftleftdemo@gmail.com"
 
	/* Set password sender */
	def s2 = "Gr33nhat"
 
	/* Set email address recicpient */
	def s3 = "shiftleftdemo@gmail.com"
 
	/* Call function */
	try {
		simpleMail(s1, s2 , s3, "TITLE", "TEXT")
	}
	catch (Exception ex) {
		println ex
	}
}



emailTest()


