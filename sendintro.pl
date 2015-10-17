#!/usr/bin/perl

#
# Send direct intro message to new followers
#

use Net::Twitter;
use Storable;
use strict;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;
use List::Util 'shuffle';

my ($f_following, $f_followme, $f_donotfollow) =
  ('following.txt', 'followme.txt', 'donotfollow.txt');

my ($iam) = ('topbananaedu');


# set your own username and password here
my %consumer_tokens = (
		       consumer_key    => 'p2kbWyziLSxkVAUWXLgww',
		       consumer_secret => 'uO0EcCTzg1a1V4sFEcAIBgdKvKHZePW5uMk8CLTw7Mo'
		      );

# $datafile = oauth_desktop.dat
my (undef, undef, $datafile) = File::Spec->splitpath($0);
$datafile =~ s/\..*/.dat/;

my $twit = Net::Twitter->new (traits => [qw/API::REST RateLimit OAuth/], %consumer_tokens)
  or die "Can't connect to Twitter: $!\n";

#print Dumper($twit);
my $access_tokens = eval { retrieve($datafile) } || [];

if ( @$access_tokens ) {
  $twit->access_token($access_tokens->[0]);
  $twit->access_token_secret($access_tokens->[1]);
} else {
  my $auth_url = $twit->get_authorization_url;
  print " Authorize this application at: $auth_url\nThen, enter the PIN# provided to contunie: ";

  my $pin = <STDIN>; # wait for input
  chomp $pin;

  # request_access_token stores the tokens in $twit AND returns them
  my @access_tokens = $twit->request_access_token(verifier => $pin);

  # save the access tokens
  store \@access_tokens, $datafile;
}

# Everything's ready

print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";
my $sleep_time = 0;

#
# read from a file the ids that followed last time around
#
my %followedmethen = ();
my @followedmethen = ();

if (-f $f_followme) {
  print "Reading followers from db ... ";
  open (DB, $f_followme) or die "Can't read from $f_followme: $!\n";
  @followedmethen = <DB>;
  close (DB);
  chomp @followedmethen;
  print "[" . ($#followedmethen+1) . "]\n";
  foreach my $rec (@followedmethen) {
    $followedmethen{$rec} = 0;
  }
} else {
  print "Followers db doesn't exist and will be created ...\n";
}

my @followmenew = ();
my @followmenow = ();
my $dmcnt = 0;
eval {
  print "Reading current followers from Twitter ... ";
  for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    $r = $twit->followers_ids({ cursor => $cursor }) or
      die "Can't fetch followers of $iam: $!\n";
    push @followmenow, @{ $r->{ids} };
  }
  print "[" . ($#followmenow+1) . "]\n";

  #
  # find new followers
  #
  foreach my $id (@followmenow) {
    if (!exists($followedmethen{$id})) {
      push (@followmenew, $id);
    } else {
      $followedmethen{$id} = 1;
    }
  }
  print "Found " . ($#followmenew+1) . " new followers\n";
};
if ( $@ ) {
  die "---> $@\n";
}

#
# look for former followers who dropped out
#
open (DNF, ">>${f_donotfollow}") or die "Can't open $f_donotfollow for append: $!\n";
foreach my $id (keys(%followedmethen)) {
  if (!$followedmethen{$id}) {
    eval {
      $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
      my $usr = $twit->show_user($id);
      print "DNF -> " . $usr->{screen_name} . "\n";
      print DNF $id . "\n";
    };
    if ( $@ ) {
      warn "---> Something went wrong: $@\n";
    }
  }
}
close(DNF);

#
# Send direct messages to new followers
# limit it to 100 DM's a day
#
splice(@followmenew, 100) if (@followmenew > 100);
foreach my $id (@followmenew) {
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    my $usr = $twit->show_user($id);
    print "DM --> " . $usr->{screen_name} . "\n";
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    $twit->new_direct_message($id,
			      "Thanks for following. Please help spread the word about our Foundation. Join us on Causes.com at http:\/\/ow.ly\/3ugEm"
			     );

    ++$dmcnt;
  };
  if ( $@ ) {
    warn "---> Something went wrong: $@\n";
  }
}


if ( $dmcnt ) {
  splice (@followmenew, $dmcnt) if ($dmcnt < @followmenew);
  #
  # Finally, update the DB of current followers
  #
  if (@followmenew) {
    #
    # back up the existing db file
    #
    if (-f $f_followme) {
      my @timelist = localtime();
      my $tstamp = sprintf("%04d%02d%02d%02d%02d%02d",
			   1900+$timelist[5],$timelist[4]+1,$timelist[3],
			   $timelist[2],$timelist[1],$timelist[0]
			  );
      print "Backing up db: $f_followme -> " . join('.', (${f_followme},${tstamp})) . "\n";
      rename($f_followme, join('.', (${f_followme},${tstamp})));
    }

    # append new followers to the db file
    print "Appending new followers to the db\n";
    open (DB, "> $f_followme") or die "Can't write to $f_followme: $!\n";
    foreach my $rec (@followmenow) {
      print DB $rec . "\n";
    }
    close (DB);
  }
}

print "Done.\n";

exit 0;
