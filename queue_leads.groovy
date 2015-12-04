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
queueUpLeadFollowers(dbQueueLeadsBatchSize())
System.exit(0)


def queueUpLeadFollowers(max) {
	def leadId = dbNextLead()
	if (leadId == null) {
		println "No users to follow ... done."
		return
	}

	def lead = twitter.lookupUsers(leadId).first()
	println "Followers of " + lead.screenName

	def nextCursor = -1
	def idsToFollow = []
	while (idsToFollow.nextCursor != 0) {
		idsToFollow = twitter.getFollowersIDs(leadId, nextCursor)
		//println idsToFollow.ids
		nextCursor = idsToFollow.nextCursor

		for (id in idsToFollow.ids) {
			if (id != me.id) {
				if (! dbQueuedUpToFollow(id)) {
					def user = twitter.lookupUsers(id).first()
					println "Queueing up " + user.screenName + " to be followed..."
					dbQueueFollow(id, user.screenName, user.name)
					if (--max == 0) {
						break
					}
				}
			}
		}
	}

	// Followed all the lead's followers for now
	dbLeadQueueCompleted(leadId)

}



