@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')

import twitter4j.*
import TwitterWrapper

def twitter = new TwitterWrapper()

// @handle
def leadHandle = this.args.getAt(0)

def me = twitter.verifyCredentials()

def lead = twitter.lookupUser(leadHandle)
println "Followers of " + [lead.id, lead.screenName, lead.name].join(" | ")

def nextCursor = -1
def idsToFollow = []
while (idsToFollow.nextCursor != 0) {
	idsToFollow = twitter.getFollowersIDs(lead.id, nextCursor)
	// println idsToFollow.ids
	nextCursor = idsToFollow.nextCursor

	for (id in idsToFollow.ids) {
		if (id != me.id) {
			def user = twitter.lookupUser(id)
			println ([user.id, user.screenName, user.name, 'null', 'null', 'false'].collect {
				(it instanceof Long) ? it : '"' + it + '"'
			}.join(","))
		}
	}

}


