@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*


twitter = TwitterFactory.getSingleton()

// Add the API rate status listener
twitter.addRateLimitStatusListener (new RateLimitStatusListener() {
	void onRateLimitStatus(RateLimitStatusEvent e) {
		RateLimitStatus status = e.getRateLimitStatus()
		// println "RateLimitStatus: " + status
		if (status.remaining < 2) {
			def sleepTime = status.getSecondsUntilReset() + 5
			print "API rate limit reached. Sleeping for " + sleepTime + " sec(s) ..."
			sleep(sleepTime * 1000)
			println " done"
		}
	}

	void onRateLimitReached(RateLimitStatusEvent e) {}
})


me = twitter.verifyCredentials()

def nextCursor = -1
def idsFriends = []
while (idsFriends.nextCursor != 0) {
	idsFriends = twitter.getFriendsIDs(nextCursor)
	// println idsFriends.ids
	nextCursor = idsFriends.nextCursor

	for (id in idsFriends.ids) {
		def user = twitter.lookupUsers(id).first()
		println "Updating " + user.screenName
		dbFollow(id, user.screenName, user.name)
	}

}

