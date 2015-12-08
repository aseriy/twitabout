@Grab(group='mysql', module='mysql-connector-java', version='5.1.+')

import java.sql.*
import java.util.Date


class common_db {

	def static db_name = 'twitabout'
	def static db_user = 'rational'
	def static db_pass = 'Gr33nhat'


	def static dbAllFollows() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

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


	def static dbQueueFollow (id, screen_name, name = null) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		// Return this to indicate wheather queuing up for follow was possible or not
		def didIt = false

		// Check if the Twitter is already in the DB. It's possible that we had followed/un-followed
		// this user in the past. If we did, forego following again.
		def sql = "SELECT id FROM follow_log WHERE id=${id}"
		ResultSet rs = stmt.executeQuery(sql)
		if (!rs.first()) {
			sql = "INSERT INTO follow_log (id, screen_name"
			if (name != null) {	
				sql = sql + ", name"
			}
			sql = sql + ") VALUES (" + id + ",\"" + screen_name + "\""
			if (name != null) {
				sql = sql + ",\"" + name + "\""
			}
			sql = sql + ")"

			stmt.executeUpdate(sql)
			didIt = true
		}

		conn.close()
		return didIt
	}


	def static dbQueuedUpToFollow (id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()
		def sql = "SELECT * FROM follow_log WHERE id = " + id

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
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def sql = "UPDATE follow_log SET persist=TRUE WHERE id=${id}"
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbFollow (id, screen_name, name = null) {
		def sql = ''

		// Check if already queued up to be followed
		if (dbQueuedUpToFollow(id)) {
			sql = "UPDATE follow_log SET followed_on = now() WHERE id = " + id

			// Fix: if the 'name' wasn't set before, update it now
			if (name != null) {
				sql = "UPDATE follow_log SET followed_on = now(), name = " + "\"" + name + "\" WHERE id = " + id
			}

		} else {
			sql = "INSERT INTO follow_log (id, screen_name"
			if (name != null) {
				sql = sql + ", name"
			}
			sql = sql + ", followed_on) VALUES (" + id + ",\"" + screen_name + "\""
			if (name != null) {
				sql = sql + ",\"" + name + "\""
			}
			sql = sql + ",now())"
		}

		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbUnfollow (id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def sql = "UPDATE follow_log SET unfollowed_on=now() WHERE id=" + id

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbDeleteFollow(id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()
		def sql = "DELETE FROM follow_log WHERE id = " + id
		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbAlreadyFollowed(id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

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


	def static dbCanUnfollow(id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

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


	def static dbIdsToFollow() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()
		def sql = "SELECT value FROM limits WHERE param = 'follow_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')

		sql = "SELECT id FROM follow_log WHERE followed_on IS NULL LIMIT " + limit
		rs = stmt.executeQuery(sql)
		rs.beforeFirst()

		def ids2follow = []
		while (rs.next()) {
			ids2follow.add(rs.getLong('id'))
		}

		conn.close()
		return ids2follow
	}


	def static dbUnfollowBatchSize() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def sql = "SELECT value FROM limits WHERE param = 'unfollow_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')
		
		conn.close()
		return limit
	}

	def static dbIdsToUnfollow() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def idsToUnfollow = []

		def sql = "SELECT value FROM limits WHERE param='days_before_unfollow'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def retention = rs.getInt('value')

		def today = new Date()		
		def followedBefore = (today - retention).format("YYYY-MM-dd")
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
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def today = new Date()
		def sql = "SELECT COUNT(*) FROM follow_log WHERE followed_on >= '" + today.format("YYYY-MM-dd") + "'"

		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def followedToday = rs.getLong('count(*)')

		sql = "SELECT value FROM limits WHERE param='max_daily_follows'"
		rs = stmt.executeQuery(sql)
		rs.first()
		def dailyLimit = rs.getInt('value')

		def retval
		if (followedToday < dailyLimit)
			retval = false
		else
			retval = true

		conn.close()
		return retval
	}


	def static dbDailyUnfollowLimitReached() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def today = new Date()
		def sql = "SELECT COUNT(*) FROM follow_log WHERE unfollowed_on >= '" + today.format("YYYY-MM-dd") + "'"

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
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def sql = "SELECT id FROM leads ORDER BY last_queued LIMIT 1"
		ResultSet rs = stmt.executeQuery(sql)
		if (!rs.first())
			return null

		def lead = rs.getLong('id')
		conn.close()

		return lead
	}


	def static dbQueueLeadsBatchSize() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()

		def sql = "SELECT value FROM limits WHERE param = 'queue_leads_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()
		def limit = rs.getInt('value')
		
		conn.close()
		return limit
	}


	def static dbLeadQueueCompleted(id) {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

		Statement stmt = conn.createStatement()
		def sql = "UPDATE leads SET last_queued = now() WHERE id = " + id

		stmt.executeUpdate(sql)
		conn.close()
	}


	def static dbGetInfluencers() {
		Connection conn = DriverManager.getConnection(
						"jdbc:mysql://localhost/" + db_name + "?user=" + db_user + "&password=" + db_pass)

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

}

