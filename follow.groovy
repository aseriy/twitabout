@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import static common_utils.*
import TwitterWrapper


twitter = new TwitterWrapper()
me = twitter.me
batchFollow((24 / dbDailyFollowBatches() + 0.5).toInteger())
System.exit(0)


def batchFollow(timePeriod) {
	def idsToFollow = dbIdsToFollow()
	def delaySchedule = randomSchedule(timePeriod, idsToFollow.size())

	for (def i = 0; i < idsToFollow.size(); i++) {
		def id = idsToFollow.getAt(i)

		if (dbDailyFollowLimitReached()) {
			println "Daily follow limit reached"
			break
		}
		if (should_follow(id)) {
			follow_someone(id)
		}

		// Slow down for a random period
		println "Next follow in ${secsToHuman(delaySchedule.getAt(i))}"
		sleep(1000 * delaySchedule.getAt(i))
	}

}


def follow_someone(id) {
	def user = twitter.lookupUser(id)
	println "Following " + user.screenName + " ..."
	try {
		twitter.createFriendship(id)
		dbFollow(id)
	}
	catch (TwitterException ex) {
		println "Exception: " + ex
		if (ex.statusCode == 403) {
			println "Twitter limit following has been reached ..."
			//TODO: Handle limitation on the number of follows
			//TODO: This can only be the the user has protected twits

			//TwitterException{exceptionCode=[422431fa-182e52d2], statusCode=403, message=To protect our users from spam and other malicious activity, this account is temporarily locked. Please log in to https://twitter.com to unlock your account., code=326, retryAfter=-1, rateLimitStatus=null, version=4.0.4}
		}
	}
}


def should_follow(id) {
	// Exclude myself
	if (id == me.id) return false
    
	def user = null

	try {
		user = twitter.lookupUser(id)
		// println user.name
	}

	// If no user found, it could be that the user disappeared from Twitter
	// from the time it was queued up to be followed. Delete it from our DB.
	catch (TwitterException ex) {
		// println "Exception: " + ex
		if (ex.statusCode == 404) {
			println "User " + id + " no longer exists ..."
			dbDeleteFollow(id)
		}

		return false
	}

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


