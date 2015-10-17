#!/usr/bin/perl

#
# given a twitter ID(s), follow X followers
#

use Net::Twitter;
use strict;
use Data::Dumper;
use Storable;
use File::Spec;
use Net::Twitter::Role::RateLimit;
use List::Util 'shuffle';

my $f_donotfollow = 'donotfollow.txt';
my ($count, @ids2follow) = @ARGV;

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
# print Dumper($twit);
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

my ($user) = ('topbananaedu');

print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";
my $sleep_time = 0;


my @ifollow = ();
for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
  $r = $twit->following_ids({ cursor => $cursor }) or
    die "Can't fetch followings of $user ids: $!\n";
  push @ifollow, @{ $r->{ids} };
}
print "I'm following: " . ($#ifollow+1) . "\n";


my @followme = ();
for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
  $r = $twit->followers_ids({ cursor => $cursor }) or
    die "Can't fetch followers of $user ids: $!\n";
  push @followme, @{ $r->{ids} };
}
print "I'm followed by: " . ($#followme+1) . "\n";

my @followers = ();
foreach my $id2follow (@ids2follow) {
  for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    $r = $twit->followers_ids($id2follow, { cursor => $cursor }) or
      die "Can't fetch followers of $id2follow ids: $!\n";
    push @followers, @{ $r->{ids} };
  }
}

my $ids2follow = join(', ', @ids2follow);
print "${ids2follow} followed by: " . ($#followers+1) . "\n";

#
# eliminate those followers in common
#
my %donotfollow = ();
foreach my $id (@ifollow, @followme) {
  $donotfollow{$id} = 1;
}
my @follow = ();
my $incommon = 0;
foreach my $id (@followers) {
  if (!exists($donotfollow{$id})) {
    push (@follow, $id);
  } else {
    ++$incommon;
  }
}

print "Already following or followed by $incommon followers of $ids2follow\n";

#
# Avoid following those on the DoNotFollow list
#
open (DNF, $f_donotfollow) or die "Can't open $f_donotfollow: $!\n";
print "Reading DoNotFollow list ... ";
my @donotfollow = <DNF>;
chomp @donotfollow;
close(DNF);
print "[" . ($#donotfollow+1) . "]\n";
foreach my $id (@donotfollow) {
  $donotfollow{$id} = 1;
}

my %follow = ();
foreach my $id (@follow) {
  $follow{$id} = 1;
}
@follow = keys %follow;
my $followcount = @follow;
@follow = ();
foreach my $id (keys %follow) {
  push (@follow, $id)if (!exists($donotfollow{$id}));
}
my $dnfcount = @follow;
$dnfcount = $followcount - $dnfcount;
print "Skipping $dnfcount user on the DoNotFollow list ...\n";

#
# shuffle the list to avoid running into the ones we've already requested
# to follow in the previous runs over and over again
#
@follow = shuffle @follow;

#
# now make new friends
#
my $i = 0;
if ($count < @follow) {
  splice (@follow, $count);
} else {
  $count = @follow;
}
print "Following $count users ...\n";

foreach my $id (@follow) {
  my $usr = undef;
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    $usr = $twit->show_user($id);
  };
  if ( $@ ) {
    warn "---> $@\n";
    next;
  }

  $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
  print ++$i . ": $id -> " . $usr->{screen_name} . "\n";
  eval {
    $twit->create_friend($id);
  };
  if ( $@ ) {
    warn "---> update failed because: $@\n";
    if ($@ =~ /Could not follow user: You've already requested to follow ([^\.]+)\.|Could not follow user: You have been blocked from following this account/) {
      my $dnf = $1;
      print "Adding $dnf [$id] to the Do Not Follow list ...\n";
      open (DNF, ">>${f_donotfollow}") or die "Can't open $f_donotfollow for append: $!\n";
      print DNF "$id\n";
      close(DNF);
    }
    if ($@ =~ /Could not follow user: You are unable to follow more people at this time\./) {
      print "Exiting ...\n";
      last;
    }
  }
}

exit 0;

