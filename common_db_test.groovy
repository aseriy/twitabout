import static common_db.*


def dbQueueFollowTest() {
	def toQueue = [
		[id: 297137315, screen_name: 'claudehanhart', name: 'Claude Hanhart'],
		[id: 606525772, screen_name: 'apriendeau'],
		[id: 14589903, screen_name: 'idclare'],
		[id: 927645660, screen_name: 'pullu84', name: 'Roberto Pullicino'],
		[id: 3041486088, screen_name: 'CoolanCo', name: 'Coolan'],
		[id: 165759168, screen_name: 'OvertureNews', name: 'Overture Networks'],
		[id: 181146136, screen_name: 'FabioPirrellos', name: 'Fabio Pirrello']
	]

	toQueue.each {
		println "Queuing up " + it.screen_name
		if (it.name) {
			dbQueueFollow(it.id, it.screen_name, it.name)
		} else {
			dbQueueFollow(it.id, it.screen_name)
		}
	}
}


/*
 * Run this after dbQueueFollowTest()
 */
def dbQueuedUpToFollowTest() {
	println dbQueuedUpToFollow(297137315)
	println dbQueuedUpToFollow(927645660)
	println dbQueuedUpToFollow(181146136)
	println dbQueuedUpToFollow(14589903)
	println dbQueuedUpToFollow(9999999999)
}


def dbFollow1Test() {
	def toFollow = [
		[id: 297137315, screen_name: 'claudehanhart', name: 'Claude Hanhart'],
		[id: 606525772, screen_name: 'apriendeau', name: 'Austin Riendeau']
	]

	toFollow.each {
		println "Following " + it.screen_name
		dbFollow(it.id, it.screen_name, it.name)
	}
}


def dbFollow2Test() {
	def toFollow = [
		[id: 1321960278, screen_name: 'aga_sumit', name: 'Sumit Agarwal'],
		[id: 1589686249, screen_name: 'VMVernak'],
		[id: 3195471973, screen_name: 'simoncasey1982', name: 'Simon Casey'],
		[id: 176351741, screen_name: 'kanyarichuru', name: 'saka churu'],
		[id: 3187914648, screen_name: 'krismy93', name: 'Krismy Alfaro'],
		[id: 3246847954, screen_name: 'TekLuv_Channing', name: 'Channing Griffin']
	]

	toFollow.each {
		println "Following " + it.screen_name
		if (it.name) {
			dbFollow(it.id, it.screen_name, it.name)
		} else {
			dbFollow(it.id, it.screen_name)
		}
	}
}


/*
 * Run this after dbFollow2Test()
 */
def dbAlreadyFollowedTest() {
	println dbAlreadyFollowed(1321960278)
	println dbAlreadyFollowed(3195471973)
	println dbAlreadyFollowed(0)
	println dbAlreadyFollowed(22700070)
}


def dbIdsToFollowTest() {
	println dbIdsToFollow()
}


def dbDailyFollowLimitReachedTest() {
	println dbDailyFollowLimitReached()
}


def dbNextLeadTest() {
	println dbNextLead()
}


def dbGetInfluencersTest() {
	println dbGetInfluencers()
}

def dbAllFollowsTest() {
	def allFollows = dbAllFollows()
	println "First: " + allFollows.first()
	println "Last: " + allFollows.last()
	println "Total: " + allFollows.size()
}


def dbQueueLeadsBatchSizeTest() {
	println dbQueueLeadsBatchSize()
}


/*
	main()
*/

//println this.metaClass.methods*.name

def testToRun = this.args.getAt(0)
"${testToRun}Test"()
System.exit(0)



