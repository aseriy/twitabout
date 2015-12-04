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
		def sql = "INSERT INTO follow_log (id, screen_name"
		if (name != null) {
			sql = sql + ", name"
		}
		sql = sql + ") VALUES (" + id + ",\"" + screen_name + "\""
		if (name != null) {
			sql = sql + ",\"" + name + "\""
		}
		sql = sql + ")"

		stmt.executeUpdate(sql)
		conn.close()
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

		def sql = "SELECT value FROM limits WHERE param='queue_leads_batch_size'"
		ResultSet rs = stmt.executeQuery(sql)
		rs.first()

		def value = rs.getLong('value')
		conn.close()

		return value
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

