@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*

// @handle
def leadHandle = this.args.getAt(0)


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

def lead = twitter.lookupUsers(leadHandle)
println "Followers of " + [lead.id, lead.screenName, lead.name].join(" | ")

def nextCursor = -1
def idsToFollow = []
while (idsToFollow.nextCursor != 0) {
	idsToFollow = twitter.getFollowersIDs(lead.id, nextCursor)
	// println idsToFollow.ids
	nextCursor = idsToFollow.nextCursor

	for (id in idsToFollow.ids) {
		if (id != me.id) {
			def user = twitter.lookupUsers(id).first()
			println ([user.id, user.screenName, user.name, 'null', 'null', 'false'].collect {
				(it instanceof Long) ? it : '"' + it + '"'
			}.join(","))
		}
	}

}


