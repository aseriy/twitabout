@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_twitter.*


def lookupUserTest() {
	println lookupUser(twitter, 4253059829)
	println lookupUser(twitter, 208586904)
}

/*
	main()
*/
twitter = TwitterFactory.getSingleton()
//println this.metaClass.methods*.name

def testToRun = this.args.getAt(0)
"${testToRun}Test"()
System.exit(0)



