@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.4')

import twitter4j.*
import static common_db.*

class common_twitter {

	def static lookupUser (twitter, id) {
		def user = dbGetUser(id)
		if (user == null) {
			try {
				def tUser = twitter.lookupUsers(id).first()
				user = [id: tUser.id, screenName: tUser.screenName, name: tUser.name]
				dbPutUser(tUser.id, tUser.screenName, tUser.name)
			}
			catch (TwitterException ex) {
				println ex
			}
		}

		return user
	}

}

