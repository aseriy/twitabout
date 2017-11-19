@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')

import twitter4j.*
import static common_db.*


class TwitterWrapper {
	def twitter
	def me

	TwitterWrapper() {
		twitter = TwitterFactory.getSingleton()
		me = twitter.verifyCredentials()

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
	}

	
	def verifyCredentials() {
		twitter.verifyCredentials()
	}

	def getFriendsIDs(cur) {
		twitter.getFriendsIDs(cur)
	}

	def getFollowersIDs(cur) {
		twitter.getFollowersIDs(cur)
	}

	def getFollowersIDs(id, cur) {
		twitter.getFollowersIDs(id, cur)
	}

	def createFriendship(id) {
		twitter.createFriendship(id)
	}

	def destroyFriendship(id) {
		def user = null

		try {
			user = twitter.destroyFriendship(id)
			//println "User: " + user
		}
		catch (TwitterException ex) {
			//println "Exception: " + ex
			if (ex.statusCode == 404) {
				dbDeleteUser(id)
			}
		}

		return user
	}

	def lookupUser (id, useCache = true) {
		def user = useCache ? dbGetUser(id) : null
		if (user == null) {
			try {
				def tUser = twitter.lookupUsers(id).first()
				//println tUser
				dbPutUser(tUser)
				user = dbGetUser(id)
			}
			catch (TwitterException ex) {
				//println ex
				if (ex.statusCode == 404) {
					dbDeleteUser(id)
				}
			}
		}

		return user
	}


	def lookupFriendship(id) {
		twitter.lookupFriendships(id).getAt(0)
	}
}

