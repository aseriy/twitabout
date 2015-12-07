/*
TABLE: follow_log
*/
ALTER TABLE follow_log MODIFY screen_name varchar(20) NOT NULL UNIQUE;
ALTER TABLE follow_log MODIFY followed_on timestamp NULL;
ALTER TABLE follow_log MODIFY unfollowed_on timestamp NULL;
ALTER TABLE follow_log MODIFY persist boolean DEFAULT false;

/*
TABLE: leads
*/
ALTER TABLE leads MODIFY screen_name varchar(20) NOT NULL UNIQUE;
ALTER TABLE leads MODIFY last_queued timestamp NULL;


