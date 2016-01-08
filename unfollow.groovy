@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import static common_twitter.*


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
batchUnfollow()
System.exit(0)


def batchUnfollow() {
	def batchSize = dbUnfollowBatchSize()

	for (id in dbIdsToUnfollow()) {
		if (dbDailyUnfollowLimitReached()) {
			println "Daily un-follow limit reached"
			break
		}
		if (!dbAlreadyFollower(id)) {
			unfollow_someone(id)
			if (--batchSize == 0) {
				return
			}
		}
	}

}


def unfollow_someone(id) {
	def user = lookupUser(twitter, id)
	println "Un-following " + user.screenName + " ..."
	try {
		twitter.destroyFriendship(id)
		dbUnfollow(id)
	}
	catch (TwitterException ex) {
		// println "Exception: " + ex
	}
}


def should_unfollow(id) {
	def user = null
	def retval = false

	try {
		user = lookupUser(twitter, id)
		// println user.name

		// Only unfollow those who haven't followed us back
		def friendship = twitter.lookupFriendships(id).getAt(0)
		println friendship
		retval = !friendship.followedBy
	}

	// If no user found, it could be that the user disappeared from Twitter
	// from the time it was followed. Delete it from our DB altogether.
	catch (TwitterException ex) {
		// println "Exception: " + ex
		if (ex.statusCode == 404) {
			println "User " + id + " no longer exists ..."
			dbDeleteFollow(id)
		}

		return false
	}

	return retval
}


