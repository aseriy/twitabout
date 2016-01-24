@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper

def leadHandle = this.args.getAt(0)

def twitter = new TwitterWrapper()

def user = twitter.lookupUser(leadHandle)
if (dbGetLead(user.id) == null) {
	dbPutLead(user.id)
}


