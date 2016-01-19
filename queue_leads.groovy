@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import static common_db.*
import TwitterWrapper


twitter = new TwitterWrapper()
me = twitter.me
queueUpLeadFollowers(dbQueueLeadsBatchSize())
System.exit(0)


def queueUpLeadFollowers(max) {
	def leadId = dbNextLead()
	if (leadId == null) {
		println "No users to follow ... done."
		return
	}

	def batchSize = dbQueueLeadsBatchSize()
	println "batchSize: " + batchSize

	def lead = twitter.lookupUser(leadId)
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
					def user = twitter.lookupUser(id)
					if (dbQueueFollow(id)) {
						println "Queueing up " + user.screenName + " to be followed..."
						--max
						--batchSize
					} else {
						println "${user.screenName} was followed & un-followed in the past." 
					}

					if (max == 0 || batchSize == 0) {
						nextCursor = 0
						break
					}
				}
			}
		}
	}

	// Followed all the lead's followers for now
	dbLeadQueueCompleted(leadId)

}



