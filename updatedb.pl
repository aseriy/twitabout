#!/usr/bin/perl

#
# Send direct intro message to new followers
#

use Net::Twitter;
use strict;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;
use List::Util 'shuffle';



my ($iam, $mypasswd) = ('topbananaedu', 'grabmathn0w');

my ($f_following, $f_followme) = ('following.txt', 'followme.txt');


my $twit = Net::Twitter->new (traits => [qw/API::REST RateLimit/],
			      username=>$iam,
			      password=>$mypasswd) or
  die "Can't connect to Twitter: $!\n";
#print Dumper($twit);


print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";
my $sleep_time = 0;


my @followme = ();
eval {
  for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    $r = $twit->followers_ids({ cursor => $cursor }) or
      die "Can't fetch followers of $iam: $!\n";
    push @followme, @{ $r->{ids} };
  }
};
if ( !$@ ) {
  #
  # back up the existing db file
  #
  if (-f $f_followme) {
    my @timelist = localtime();
    my $tstamp = sprintf("%04d%02d%02d%02d%02d%02d",
			 1900+$timelist[5],$timelist[4]+1,$timelist[3],
			 $timelist[2],$timelist[1],$timelist[0]
			);
    print "$tstamp \n";
    rename($f_followme, join('.', (${f_followme},${tstamp})));
  }

  open (DB, "> $f_followme") or die "Can't write to $f_followme: $!\n";
  foreach my $rec (@followme) {
    print DB $rec . "\n";
  }
  close (DB);
} else {
  die "---> update failed because: $@\n";
}

exit 0;
