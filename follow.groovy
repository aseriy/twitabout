@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper


twitter = new TwitterWrapper()
me = twitter.me
batchFollow()
System.exit(0)


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
	def user = twitter.lookupUser(id)
	println "Following " + user.screenName + " ..."
	try {
		twitter.createFriendship(id)
		dbFollow(id)
	}
	catch (TwitterException ex) {
		//println "Exception: " + ex
		if (ex.statusCode == 403) {
			println "Twitter limit following has been reached ..."
			//TODO: Handle limitation on the number of follows
			//TODO: This can only be the the user has protected twits
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


