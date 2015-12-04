#!/usr/bin/perl

use List::Util 'shuffle';
use Net::Twitter;
use strict;
use Data::Dumper;
use Net::Twitter::Role::RateLimit;

my $f_locals = 'locals.txt';
my $listplus = 'greater-boston';

# set your own username and password here
my $user = 'topbananaedu';
my $password = 'grabmathn0w';

#
# read the list of local twitters
#
my @locals = ();
open(IN, $f_locals) or die "Can't read from $f_locals: $!\n";
@locals = <IN>;
close(IN);
chomp @locals;
my %locals = ();
foreach my $x (@locals) {
  $locals{$x} = 1;
}
@locals = ();

my @followers = ();
my $twit = Net::Twitter->new (
			      traits => [qw/API::REST API::Lists RateLimit/],
			      username=>$user,
			      password=>$password
			     ) or
  die "Can't connect to Twitter: $!\n";
print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";
my $sleep_time = 0;

#
# fetch the user that follow us
#
for ( my $cursor = -1, my $r; $cursor; $cursor = $r->{next_cursor} ) {
  $sleep_time = $twit->until_rate(1.0); print "Sleeping for $sleep_time ...\n"; sleep($sleep_time);
  $r = $twit->followers_ids({ cursor => $cursor }) or
    die "Can't fetch followers' ids: $!\n";
  push @followers, @{ $r->{ids} };
}
print "Followed by: " . ($#followers+1) . "\n";

my $i = 0;
foreach my $id (shuffle @followers) {
  eval {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    my $usr = $twit->show_user($id);
    print ++$i . ": $id -> $usr->{screen_name} ($usr->{location})";
    if (
	exists($locals{$usr->{location}}) or
	$usr->{location} =~ /\,\s+MA\s*$/ or
	$usr->{location} =~ /Massachusetts/
       ) {
      $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
      $twit->add_list_member ($user, $listplus, $id);
      print " ... ++++++";
    }
    print "\n";
  }; # end of eval{}
  if ( $@ ) {
    warn "---> error: $@\n";
  }
}

