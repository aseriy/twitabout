@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')
@Grab(group = 'com.opencsv', module = 'opencsv', version = '3.6')

import twitter4j.*
import static common_db.*
import TwitterWrapper

import com.opencsv.CSVReader
import com.opencsv.CSVParser

def twitter = new TwitterWrapper()

def csvReader = new FileReader(this.args.getAt(1))
CSVReader reader = new CSVReader(csvReader)
def ids = reader.readAll().collect {it.getAt(0) as Long}
def leadId = twitter.lookupUser(this.args.getAt(0) as String)
ids.add(leadId.id)

me = twitter.verifyCredentials()

// Follow
//
for (def i = 0; i < ids.size(); i++) {
	def id = ids[i]
	try {
		println "Following " + id		
		twitter.createFriendship(id)
		println "Un-following " + id		
		twitter.destroyFriendship(id)
	}
	catch (TwitterException ex) {
		println "Exception: " + ex
	}
}


