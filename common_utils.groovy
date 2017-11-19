import java.util.Date


class common_utils {

	// Generate a schedule in a form of a sequence of
	// random sleep time between repeated execution of a
	// procedure.
	// Arguments
	// p: the period of time in hours to fit the schedule into
	// n: the number of events on the schedule     
	//
	def static randomSchedule(p, n) {
		def schedule = []

		def rand = new Random()

		(1..n).each {
			schedule.add(rand.nextInt(1000))
		}

		def sum = schedule.sum()
		def totalTime = p * 3600

		for (def i = 0; i < n; i++) {
			schedule[i] = (((schedule[i] * totalTime).toDouble() / sum) + 0.5).toInteger()
		}
	
		return schedule
	}

	def static secsToHuman(sec) {
		def hms = [
			'H': (sec/3600).toInteger(),
			'M': (sec/60).toInteger() % 60,
			'S': sec % 60
		]

		sprintf ("%02d:%02d:%02d", hms.H, hms.M, hms.S)
	}


	// Escape all special characters in a given string
	// and wrap the result in double-quotes
	//
	def static doubleQuote(s) {
		'"' + s.replace('\\','\\\\').replace('"','\\"') + '"'
	}
}

