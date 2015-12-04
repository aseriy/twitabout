#!/usr/bin/perl

use Net::Twitter;
use strict;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;

my $f_donotfollow = 'donotfollow.txt';
my $f_dnflang = 'donotfollow.lang';

# set your own username and password here
my $user = 'topbananaedu';
my $password = 'grabmathn0w';

#
# read the list of languages to exclusion
#
my @dnflang = ();
open(DNF, $f_dnflang) or die "Can't read from $f_dnflang: $!\n";
@dnflang = <DNF>;
close(DNF);
chomp @dnflang;
my %dnflang = ();
foreach my $x (@dnflang) {
  $dnflang{$x} = 1;
}
@dnflang = ();

my @following = ();
my $twit = Net::Twitter->new (traits => [qw/API::REST RateLimit/],
			      username=>$user,
			      password=>$password) or
  die "Can't connect to Twitter: $!\n";
#print Dumper($twit);


print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";

my $sleep_time = 0;

#
# fetch the user we follow
#
for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); print "Sleeping for $sleep_time ...\n"; sleep($sleep_time);
  $r = $twit->following_ids({ cursor => $cursor }) or
    die "Can't fetch following ids: $!\n";
  push @following, @{ $r->{ids} };
}
print "Following: " . ($#following+1) . "\n";

my $i = 0;
my @donotfollow = ();
foreach my $id (reverse @following) {
  my $excluded = 0;
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    my $usr = $twit->show_user($id);
    if (exists($dnflang{$usr->{lang}})) {
      print ++$i . ": $id -> $usr->{screen_name} ($usr->{lang}) ... unfollowing\n";
      $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
      $twit->destroy_friend($id);
      $excluded = 1;
    } else {
      print ++$i . ": $id -> $usr->{screen_name} ($usr->{lang})\n";
    }
  }; # end of eval{}
  if ( $@ ) {
    warn "---> update failed because: $@\n";
  } else {
    push (@donotfollow, $id) if ($excluded);
  }

  if (@donotfollow == 10 or $id == $following[$#following]) {
    open (DNF, ">>${f_donotfollow}") or die "Can't open $f_donotfollow for append: $!\n";
    foreach my $dnf (@donotfollow) {
      print DNF "$dnf\n";
    }
    close(DNF);
    @donotfollow = ();
  }
}

