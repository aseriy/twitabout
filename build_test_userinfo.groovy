@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.2')
@Grab(group = 'com.opencsv', module = 'opencsv', version = '3.6')

import twitter4j.*
import static common_db.*
import TwitterWrapper

import com.opencsv.CSVReader
import com.opencsv.CSVParser

def twitter = new TwitterWrapper()

// def ids = dbAllFollows().sort { Math.random() }
// def ids = [11111111, 12345678, 10203040, 84729, 771892, 848400]
// def ids = [43164699, 47376709, 3244662354, 44978926, 3260961, 19344588]

def csvReader = new FileReader(this.args.getAt(1))
CSVReader reader = new CSVReader(csvReader)
def ids = reader.readAll().collect {it.getAt(0) as Long}

def leadId = twitter.lookupUser(this.args.getAt(0) as String)
ids.add(leadId.id)

def max = (ids.size() < 1000) ?  ids.size() : 1000
 
def me = twitter.verifyCredentials()

for (def i = 0; i < max; i++) {
	def id = ids[i]
	try {
		def user = twitter.lookupUser(id)
		def out = [id, user.screenName, user.name].join(",")
		println out
	}
	catch (TwitterException ex) {
		// println "Exception: " + ex
		if (ex.statusCode == 404) {
			// println "User " + id + " no longer exists ..."
		}
	}
}



