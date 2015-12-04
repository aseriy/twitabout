#!/usr/bin/perl

use Net::Twitter;
use strict;
use Data::Dumper;
use Storable;
use File::Spec;
use Net::Twitter::Role::RateLimit;
use List::Util 'shuffle';


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

print "Current rate limit: " . $twit->rate_limit() . "\n";
print "Calls remaining:    " . $twit->rate_remaining() . "\n";

my $sleep_time = 0;

$sleep_time = $twit->until_rate(1.0); print "Sleeping for $sleep_time ...\n"; sleep($sleep_time);
while (my $r = $twit->direct_messages()) {
  foreach my $dm (@$r) {
    $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
    eval {
      $twit->destroy_direct_message($dm->{id});
    };
    if ( $@ ) {
      warn "---> something went wrong: $@\n";
    } else {
      print "DM -> " . $dm->{id} . "\n";
    }
  }

  $sleep_time = $twit->until_rate(1.0); sleep($sleep_time);
}


