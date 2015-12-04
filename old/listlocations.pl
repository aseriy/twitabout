#!/usr/bin/perl

#
# List locations of our followers
#

use Net::Twitter;
use strict;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;

my $f_donotfollow = 'donotfollow.txt';

# set your own username and password here
my $user = 'topbananaedu';
my $password = 'grabmathn0w';

my @following = ();

my $twit = Net::Twitter->new (traits => [qw/API::REST RateLimit/],
			      username=>$user,
			      password=>$password) or
  die "Can't connect to Twitter: $!\n";
#print Dumper($twit);


print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";

my $sleep_time = 0;

for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); print "Sleeping for $sleep_time ...\n"; sleep($sleep_time);
  $r = $twit->following_ids({ cursor => $cursor }) or
    die "Can't fetch following ids: $!\n";
  push @following, @{ $r->{ids} };
}
print "Following: " . ($#following+1) . "\n";

#
# info to be collected
#
my %location = ();
my $f_location = "following.location";
my %lang = ();
my $f_lang = "following.lang";
my %time_zone = ();
my $f_timezone = "following.timezone";
my %profile_img = ();
my $f_profileimg = "following.profileimg";

foreach my $id (@following) {
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    my $usr = $twit->show_user($id);
#    print Dumper($usr);
    print $id . " -> " . $usr->{screen_name} . "\n";

    # location
    $location{$usr->{location}} = 1 if (!exists ($location{$usr->{location}}));

    # language
    $lang{$usr->{lang}} = 1 if (!exists ($lang{$usr->{lang}}));

    # time zone
    $time_zone{$usr->{time_zone}} = 1 if (!exists ($time_zone{$usr->{time_zone}}));

    # profile image
    $profile_img{$usr->{profile_image_url}} = 1 if (!exists ($profile_img{$usr->{profile_image_url}}));

  };
  if ( $@ ) {
    warn "---> update failed because: $@\n";
  } else {
  }
}

#
# sort everything unique and print out
#
open(OUT, "> $f_location") or die "Can't open file $f_location: $!\n";
foreach my $x (sort keys %location) {
  print OUT $x . "\n";
}
close(OUT);

open(OUT, "> $f_lang") or die "Can't open file $f_lang: $!\n";
foreach my $x (sort keys %lang) {
  print OUT $x . "\n";
}
close(OUT);

open(OUT, "> $f_timezone") or die "Can't open file $f_timezone: $!\n";
foreach my $x (sort keys %time_zone) {
  print OUT $x . "\n";
}
close(OUT);

open(OUT, "> $f_profileimg") or die "Can't open file $f_profileimg: $!\n";
foreach my $x (sort keys %profile_img) {
  print OUT $x . "\n";
}
close(OUT);
