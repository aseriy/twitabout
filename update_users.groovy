@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper

def twitter = new TwitterWrapper()

def curOffset = 0
def userIds = dbGetUserIds(1000, curOffset)
while (userIds.size() > 0) {
	userIds.each {
		def user = twitter.lookupUser(it, false)
	}

	curOffset += userIds.size()
	userIds = dbGetUserIds(1000, curOffset)
}



