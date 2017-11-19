@Grab(group='mysql', module='mysql-connector-java', version='5.1+')

import java.sql.*
import java.util.Date
import static common_utils.*


class common_db {

	def static dbUrl = null
	def static dbUser = null
	def static dbPassword = null


	def static dbUserMap = [
		'screen_name'	: ['screenName'		, {doubleQuote(it)}],
		'name'			: ['name'			, {doubleQuote(it)}],
		'location'		: ['location'		, {doubleQuote(it)}],
		'description'	: ['description'	, {doubleQuote(it)}],
		'lang'			: ['lang'			, {doubleQuote(it)}],
		'time_zone'		: ['timeZone'		, {doubleQuote(it)}],
		'created_on'	: ['createdAt'		, {doubleQuote(it.format("yyyy-MM-dd"))}],
		'protected'		: ['isProtected'	, {it}]
	]

	def static dbInit() {
		if (dbUrl == null || dbUser == null || dbPassword == null) {
			File propFile = new File('db.properties')
			def props = new Properties()
			props.load(propFile.newDataInputStream())
			def config = new ConfigSlurper().parse(props)
			dbUrl = config.db.url
			dbUser = config.db.user
			dbPassword = config.db.password
		}
	}


	def static dbGetUserIds(limit, offset = 0) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT id FROM twitter_users ORDER BY id " +
					"LIMIT ${limit} OFFSET ${offset}"
		def rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def ids = []
		while (rs.next()) {
			ids.add(rs.getLong('id'))
		}

		conn.close()
		return ids
	}


	def static dbGetUser(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		// The id could a Long (id) or String (screen_name)
		def sql = "SELECT id, name, screen_name FROM twitter_users WHERE"
		if (id instanceof String) {
			sql = "${sql} screen_name='${id}'"
		} else {
			sql = "${sql} id=${id}"
		}
		def rs = stmt.executeQuery(sql)

		def user = null
		if (rs.first()) {
			user = [
				'id':			rs.getLong('id'),
				'screenName':	rs.getString('screen_name'),
				'name':			rs.getString('name')
			]
		}

		conn.close()
		return user
	}


	def static dbPutUser(user) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT id FROM twitter_users WHERE id=${user.id}"
		def rs = stmt.executeQuery(sql)

		if (rs.first()) {
			// Record exists - update
			sql = "UPDATE twitter_users SET "
			
			def updates = []
			dbUserMap.each { k, v ->
				if (user."${v.first()}".toString().compareTo('null') != 0) {
					updates.add("${k}=" + v.last().call(user."${v.first()}"))
				}
			}
			sql += updates.join(", ") + " WHERE id=${user.id}"
		} else {
			// Record doesn't exist - create new
			def columns = []
			def values = []
			dbUserMap.each { k, v ->
				if (user."${v.first()}".toString().compareTo('null') != 0) {
					columns.add(k)
					values.add(v.last().call(user."${v.first()}"))
				}
			}

			sql = "INSERT INTO twitter_users (id, ${columns.join(", ")}) "
			sql += "VALUES (${user.id}, ${values.join(", ")})"
		}

		//println sql
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbDeleteUser(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def tables = ['follow_log','followers_log','leads','twitter_users']

		tables.each { t ->
			def sql = "DELETE FROM ${t} WHERE id=${id}"
			stmt.executeUpdate(sql)
		}

		conn.close()
	}


	def static dbAllFollows() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT id FROM follow_log"
		def rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def ids = []
		while (rs.next()) {
			ids.add(rs.getLong('id'))
		}

		conn.close()
		return ids
	}


	def static dbQueueFollow(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		// Return this to indicate wheather queuing up for follow was possible or not
		def didIt = false

		// Check if the Twitter is already in the DB. It's possible that we had followed/un-followed
		// this user in the past. If we did, forego following again.
		def sql = "SELECT id FROM follow_log WHERE id=${id}"
		ResultSet rs = stmt.executeQuery(sql)
		if (!rs.first()) {
			sql = "INSERT INTO follow_log (id) VALUES (${id})"
			stmt.executeUpdate(sql)
			didIt = true
		}

		conn.close()
		return didIt
	}


	def static dbQueuedUpToFollow (id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT * FROM follow_log WHERE id=${id}"

		ResultSet rs = stmt.executeQuery(sql)

		def retval
		if (rs.first())
			retval = true
		else
			retval = false

		conn.close()
		return retval
	}


	def static dbPersist (id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "UPDATE follow_log SET persist=TRUE WHERE id=${id}"
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbFollow (id) {
		dbInit()
		def sql = ''

		// Check if already queued up to be followed
		if (dbQueuedUpToFollow(id)) {
			sql = "UPDATE follow_log SET followed_on = now() WHERE id = ${id}"
		} else {
			sql = "INSERT INTO follow_log (id, followed_on) VALUES (${id}, now())"
		}

		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbRefollow (id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "UPDATE follow_log SET unfollowed_on=NULL WHERE id=${id}"

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbUnfollow (id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "UPDATE follow_log SET unfollowed_on=now() WHERE id=${id}"

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbDeleteFollow(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "DELETE FROM follow_log WHERE id = " + id
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbAlreadyFollowed(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT followed_on FROM follow_log WHERE id = " + id
		ResultSet rs = stmt.executeQuery(sql)

		def retval = false
		if (rs.first()) {
			if (rs.getTimestamp('followed_on') != null)
				retval = true
		}

		conn.close()
		return retval
	}


	def static dbAlreadyUnfollowed(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT unfollowed_on FROM follow_log WHERE id = " + id
		ResultSet rs = stmt.executeQuery(sql)

		def retval = false
		if (rs.first()) {
			if (rs.getTimestamp('unfollowed_on') != null)
				retval = true
		}

		conn.close()
		return retval
	}


	def static dbCanUnfollow(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT persist FROM follow_log WHERE id = " + id
		ResultSet rs = stmt.executeQuery(sql)

		def persist = false
		if (rs.first()) {
			persist = !rs.getBoolean('persist')
		}

		conn.close()
		return persist
	}


	def static dbDailyFollowBatches() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT value FROM limits WHERE param = 'max_daily_follows'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def dailyLimit = rs.getInt('value')

		sql = "SELECT value FROM limits WHERE param = 'follow_batch_size'"
		rs = stmt.executeQuery(sql)
		rs.first()
		def batchSize = rs.getInt('value')

		conn.close()
		return (dailyLimit / batchSize).toInteger()
	}


	def static dbIdsToFollow() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT value FROM limits WHERE param = 'follow_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')

		sql = "SELECT id FROM follow_log WHERE followed_on IS NULL"
		rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def ids2follow = []
		while (rs.next()) {
			ids2follow.add(rs.getLong('id'))
		}

		conn.close()
		Collections.shuffle(ids2follow)
		return ids2follow.take(limit)
	}


	def static dbUnfollowBatchSize() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT value FROM limits WHERE param = 'unfollow_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')
		
		conn.close()
		return limit
	}

	def static dbIdsToUnfollow() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def idsToUnfollow = []

		def sql = "SELECT value FROM limits WHERE param='days_before_unfollow'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def retention = rs.getInt('value')

		def today = new Date()		
		def followedBefore = (today - retention).format("yyyy-MM-dd")
		sql = "SELECT id FROM follow_log " +
				"WHERE persist IS FALSE AND " +
				"unfollowed_on IS NULL AND " +
				"followed_on < '${followedBefore}' " +
				"ORDER BY followed_on"

		rs = stmt.executeQuery(sql)
		rs.beforeFirst()
		while (rs.next()) {
			idsToUnfollow.add(rs.getLong('id'))
		}

		conn.close()
		return idsToUnfollow
	}


	def static dbDailyFollowLimitReached() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def today = new Date()
		def sql = "SELECT COUNT(*) FROM follow_log WHERE followed_on >= '" + today.format("yyyy-MM-dd") + "'"

		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def followedToday = rs.getLong('count(*)')

		sql = "SELECT value FROM limits WHERE param='max_daily_follows'"
		rs = stmt.executeQuery(sql)
		rs.first()
		def dailyLimit = rs.getInt('value')

		def retval = true
		if (followedToday < dailyLimit) {
			retval = false
		}

		conn.close()
		return retval
	}


	def static dbDailyUnfollowLimitReached() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def today = new Date()
		def sql = "SELECT COUNT(*) FROM follow_log WHERE unfollowed_on >= '" + today.format("yyyy-MM-dd") + "'"

		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def unfollowedToday = rs.getLong('COUNT(*)')

		sql = "SELECT value FROM limits WHERE param='max_daily_unfollows'"
		rs = stmt.executeQuery(sql)
		rs.first()
		def dailyLimit = rs.getInt('value')

		def retval
		if (unfollowedToday < dailyLimit)
			retval = false
		else
			retval = true

		conn.close()
		return retval
	}


	def static dbNextLead() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT id FROM leads ORDER BY RAND() LIMIT 1"
		ResultSet rs = stmt.executeQuery(sql)
		if (!rs.first())
			return null

		def lead = rs.getLong('id')
		conn.close()

		return lead
	}


	def static dbAllLeads() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT id FROM leads"
		ResultSet rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def leads = []
		while (rs.next()) {
			leads.add(rs.getLong('id'))
		}

		conn.close()
		return leads
	}


	def static dbGetLead(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def lead = null
		def user = dbGetUser(id)
		
		if (user != null) {
			def sql = "SELECT id, last_queued FROM leads WHERE id=${user.id}"
			ResultSet rs = stmt.executeQuery(sql)
			if (!rs.first())
				return null

			lead = [
					id: rs.getLong('id'),
					screenName: user.screenName,
					name: user.name,
					lastQueued: rs.getTimestamp('last_queued')
				]
			conn.close()
		}

		return lead
	}


	def static dbPutLead(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		def user = dbGetUser(id)
		def success = false

		if (user != null) {
			def lead = dbGetLead(id)
			if (lead == null) {
				Statement stmt = conn.createStatement()
				def sql = "INSERT INTO leads (id) VALUES (${id})"
				stmt.executeUpdate(sql)
			}

			success = true
		}

		return success
	}


	def static dbQueueLeadsBatchSize() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT value FROM limits WHERE param = 'queue_leads_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')
		
		conn.close()
		return limit
	}


	def static dbLeadQueueCompleted(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "UPDATE leads SET last_queued = now() WHERE id = " + id

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbGetInfluencers() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "SELECT id FROM influencers"
		ResultSet rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def influencers = []
		while (rs.next()) {
			influencers.add(rs.getLong('id'))
		}

		conn.close()
		return influencers
	}


	def static dbFollowingMe(id) {
		
	}


	def static dbFollower (id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()

		def sql = "INSERT followers_log (id) VALUES (${id})"

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbUnfollower (id) {
		if (dbAlreadyFollower(id)) {
			dbInit()
			Connection conn = DriverManager.getConnection(
							"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

			Statement stmt = conn.createStatement()

			def sql = "UPDATE followers_log SET unfollowed_on = now() WHERE id=${id} AND unfollowed_on IS NULL"

			stmt.executeUpdate(sql)
			conn.close()
		}
	}


	def static dbAllFollowers() {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT id FROM followers_log WHERE unfollowed_on IS NULL"
		def rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def ids = []
		while (rs.next()) {
			ids.add(rs.getLong('id'))
		}

		conn.close()
		return ids
	}


	def static dbAlreadyFollower(id) {
		dbInit()
		Connection conn = DriverManager.getConnection(
						"${dbUrl}?useSSL=false&user=${dbUser}&password=${dbPassword}")

		Statement stmt = conn.createStatement()
		def sql = "SELECT followed_on FROM followers_log WHERE id = ${id} AND unfollowed_on IS NULL"
		ResultSet rs = stmt.executeQuery(sql)

		def retval = false
		if (rs.first()) {
			if (rs.getTimestamp('followed_on') != null)
				retval = true
		}

		conn.close()
		return retval
	}

}

