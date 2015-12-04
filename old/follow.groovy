@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')

import twitter4j.*
import static common_db.*


twitter = TwitterFactory.getSingleton()

// Add the API rate status listener
twitter.addRateLimitStatusListener (new RateLimitStatusListener() {
	void onRateLimitStatus(RateLimitStatusEvent e) {
		RateLimitStatus status = e.getRateLimitStatus()
		// println "RateLimitStatus: " + status
		if (status.remaining == 1) {
			def sleepTime = status.getSecondsUntilReset() + 5
			print "API rate limit reached. Sleeping for " + sleepTime + " sec(s) ..."
			sleep(sleepTime * 1000)
			println " done"
		}
	}

	void onRateLimitReached(RateLimitStatusEvent e) {}
})


me = twitter.verifyCredentials()
//def statuses = twitter.getHomeTimeline() as List

batchFollow()


def batchFollow() {
	for (id in dbIdsToFollow()) {
		if (dbDailyFollowLimitReached()) {
			println "Daily follow limit reached"
			break
		}
		if (should_follow(id)) {
			follow_someone(id)
		}
	}

}


def follow_someone(id) {
	def user = twitter.lookupUsers(id).first()
	println "Following " + user.screenName
	twitter.createFriendship(id)
	dbFollow(id, user.screenName)
}


def should_follow(id) {
	// Exclude myself
	if (id == me.id) return false
    
	def user = twitter.lookupUsers(id).first()

	// TODO: allow to follow users with protected twits ... for now.
	// if user.protected:
	//	log(protected_twits=user.screen_name)
	//	return False

	if (dbAlreadyFollowed(id)) {
		return false
	} else {
		return true
	}
}

