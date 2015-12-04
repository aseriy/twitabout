#!/usr/bin/perl

use List::Util 'shuffle';
use Net::Twitter;
use strict;
use Storable;
use File::Spec;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;

#
# how many to un-follow
#
my $count = (@ARGV) ? shift @ARGV : undef;

my $f_donotfollow = 'donotfollow.txt';

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

#print Dumper $status;

# Everything's ready

my @following = ();
my @followers = ();

print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";

my $sleep_time = 0;

for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0);
  print "Sleeping for $sleep_time ...\n";
  sleep($sleep_time);

  $r = $twit->following_ids({ cursor => $cursor }) or
    die "Can't fetch following ids: $!\n";
  push @following, @{ $r->{ids} };
}
print "Following: " . ($#following+1) . "\n";

for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); print "Sleeping for $sleep_time ...\n"; sleep($sleep_time);
  $r = $twit->followers_ids({ cursor => $cursor });
  push @followers, @{ $r->{ids} };
}
print "Followers: " . ($#followers+1) . "\n";

my @unfollow = ();
my %followers = ();

foreach my $id (@followers) {
  $followers{$id} = 1;
}

foreach my $id (@following) {
  if (!exists($followers{$id})) {
    push (@unfollow, $id);
  }
}


@unfollow = reverse @unfollow;
if (defined($count)) {
  splice (@unfollow, $count) if ($count < @unfollow);
}

my $i = 0;
print "Unfollowing " . ($#unfollow+1) . " users ...\n";

my @donotfollow = ();
foreach my $id (@unfollow) {
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    print ++$i . ": $id -> ";
    my $usr = $twit->show_user($id);
    print $usr->{screen_name} . "\n";
    $twit->destroy_friend($id);
  };
  if ( $@ ) {
    warn "---> update failed because: $@\n";
  } else {
    push (@donotfollow, $id);
  }

  if (@donotfollow == 10 or $id == $unfollow[$#unfollow]) {
    open (DNF, ">>${f_donotfollow}") or die "Can't open $f_donotfollow for append: $!\n";
    foreach my $dnf (@donotfollow) {
      print DNF "$dnf\n";
    }
    close(DNF);
    @donotfollow = ();
  }
}

