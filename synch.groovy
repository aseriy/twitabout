@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper

def twitter = new TwitterWrapper()

def nextCursor = -1
def idsFriends = []
while (idsFriends.nextCursor != 0) {
	idsFriends = twitter.getFriendsIDs(nextCursor)
	// println idsFriends.ids
	nextCursor = idsFriends.nextCursor

	for (id in idsFriends.ids) {
		if (!dbAlreadyFollowed(id)) {
			def user = twitter.lookupUser(id)
			println "Following " + user.screenName
			dbFollow(id)
			dbPersist(id)
		} else if (dbAlreadyUnfollowed(id)) {
			def user = twitter.lookupUser(id)
			println "Re-following " + user.screenName
			dbRefollow(id)
			dbPersist(id)
		}
	}
}

def allIdsFollowers = []
nextCursor = -1
def idsFollowers = []
while (idsFollowers.nextCursor != 0) {
	idsFollowers = twitter.getFollowersIDs(nextCursor)
	//println idsFollowers.ids
	nextCursor = idsFollowers.nextCursor

	for (id in idsFollowers.ids) {
		if (!dbAlreadyFollower(id)) {
			def user = twitter.lookupUser(id)
			println "Followed by " + user.screenName
			dbFollower(id)
		}
	}

	allIdsFollowers.addAll(idsFollowers.ids)
}

dbAllFollowers().each { id ->
	if (allIdsFollowers.find {it == id} == null) {
		def user = twitter.lookupUser(id)
		if (user == null) {
			println "User " + id + " no longer exists, deleting ..."
		} else {
			println "${user.screenName} stopped following"
		}

		dbUnfollower(id)
	}
}




