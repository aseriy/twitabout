@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper


twitter = new TwitterWrapper()
me = twitter.me
def followBatch = dbQueueLeadsBatchSize()
dbAllLeads().each { id ->
	queueUpLeadFollowers(id, followBatch)
}
System.exit(0)


def queueUpLeadFollowers(leadId, max) {
	if (leadId == null) {
		println "No users to follow ... done."
		return
	}

	println "batchSize: ${max}"

	def lead = twitter.lookupUser(leadId)
	println "Followers of " + lead.screenName

	def nextCursor = -1
	def idsToFollow = []

	while (nextCursor != 0 && idsToFollow.size() <= max) {
		def idsToFollowBatch = twitter.getFollowersIDs(leadId, nextCursor)
		idsToFollowBatch.ids.each { id ->
			if (id != me.id) {
				if (!dbQueuedUpToFollow(id)) {
					def user = twitter.lookupUser(id)
					if (user != null) {
						println "User: ${user.screenName}"
						idsToFollow.add(id)
					}
				}
			}
		}

		nextCursor = idsToFollowBatch.nextCursor
	}

	// Trim the list to the maximum allowed
	while (idsToFollow.size() >= max) {
		idsToFollow.drop(1)
	}
	
	def actuallyQueued = 0
	idsToFollow.each { id ->
		if (dbQueueFollow(id)) {
			println "Queueing up " + user.screenName + " to be followed..."
			actuallyQueued++
			println "actuallyQueued: ${actuallyQueued}"
		} else {
			println "${user.screenName} was followed & un-followed in the past." 
		}
	}


	// If we have followed anyone, record when it was done
	//println actuallyQueued
	if (actuallyQueued > 0) {
		dbLeadQueueCompleted(leadId)
	}
}



