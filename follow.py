#!/usr/bin/env python

"""
This script is a part of the Twitabout project.

Please see the README at https://github.com/aseriy/twitabout/ for more info
"""

# imports
import os, time, json
from sys import exit
from urlparse import urlparse
from contextlib import contextmanager
import tweepy
import backoff

# import exceptions
from urllib2 import HTTPError

def log(**kwargs):
    print ' '.join( "{0}={1}".format(k,v) for k,v in sorted(kwargs.items()) )


@contextmanager
def measure(**kwargs):
    start = time.time()
    status = {'status': 'starting'}
    log(**dict(kwargs.items() + status.items()))
    try:
        yield
    except Exception, e:
        status = {'status': 'err', 'exception': "'{0}'".format(e)}
        log(**dict(kwargs.items() + status.items()))
        raise
    else:
        status = {'status': 'ok', 'duration': time.time() - start}
        log(**dict(kwargs.items() + status.items()))


def debug_print(text):
    """Print text if debugging mode is on"""
    if os.environ.get('DEBUG'):
        print text
        
def validate_env():            
    keys = [
        'TW_USERNAME',
        'TW_CONSUMER_KEY',
        'TW_CONSUMER_SECRET',
        'TW_ACCESS_TOKEN',
        'TW_ACCESS_TOKEN_SECRET',
        ]

    # Check for missing env vars
    for key in keys:
        v = os.environ.get(key)
        if not v:
            log(at='validate_env', status='missing', var=key)
            raise ValueError("Missing ENV var: {0}".format(key))

    # Log success
    log(at='validate_env', status='ok')


@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def fetch_friends(api):
    """Fetch friend list from twitter"""
    with measure(at='fetch_friends'):
        friends = api.friends_ids()
    return friends

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def fetch_followers(api):
    """Fetch followers list from twitter"""
    with measure(at='fetch_followers'):
        friends = api.followers_ids()
    return friends

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def fetch_someone_followers(api, id):
    """Fetch someone's followers list from twitter"""
    with measure(at='fetch_someone_followers'):
        friends = api.followers_ids(id)
    return friends

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def fetch_mentions(api):
    """Fetch mentions from twitter"""
    with measure(at='fetch_mentions'):
        replies = api.mentions_timeline()
    return replies

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def get_ids2follow():
    ids2follow = ['ChelsieMarieP']
    return ids2follow


follow_count = 0
def can_follow_more():
    global follow_count
    
    if follow_count == 5:
        return False
    
    follow_count += 1
    return True

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def follow_someone(api, id):
    user = api.get_user(id)
    log(following=user.screen_name)
    return True

@backoff.on_exception(backoff.expo, tweepy.TweepError, max_tries=8)
def follow_someone_followers(api):
    """Follow the specified user followers"""
    with measure(at='follow_someone_followers'):
        
        for id in get_ids2follow():
            user = api.get_user(id)
            log(followers_of=user.screen_name)
            
            for id2follow in fetch_someone_followers(api, id):
                # Check if allowed to make more new follows at this point
                if not can_follow_more():
                    return True
                    
                follow_someone(api, id2follow)
                
    return True


def main():
    log(at='main')
    main_start = time.time()

    validate_env()

    owner_username    = os.environ.get('TW_OWNER_USERNAME')
    username          = os.environ.get('TW_USERNAME')
    consumer_key      = os.environ.get('TW_CONSUMER_KEY')
    consumer_secret   = os.environ.get('TW_CONSUMER_SECRET')
    access_key        = os.environ.get('TW_ACCESS_TOKEN')
    access_secret     = os.environ.get('TW_ACCESS_TOKEN_SECRET')

    auth = tweepy.OAuthHandler(
                               consumer_key=consumer_key,
                               consumer_secret=consumer_secret
                               )
    auth.set_access_token(
                          access_key,
                          access_secret
                          )

    api = tweepy.API(auth_handler=auth, retry_count=3)
    
    ifollow = fetch_friends(api)
    followme = fetch_followers(api)
    log(at='fetched_from_api', ifollow=len(ifollow), followme=len(followme))
    follow_someone_followers(api)
    log(at='finish', status='ok', duration=time.time() - main_start)


if __name__ == '__main__':
    # set up rollbar
    rollbar_configured = False
    rollbar_access_key = os.environ.get('ROLLBAR_ACCESS_KEY')
    if rollbar_access_key:
        import rollbar
        rollbar.init(rollbar_access_key, 'production')
        rollbar_configured = True

    try:
        main()
    except KeyboardInterrupt:
        log(at='keyboard_interrupt')
        quit()
    except:
        if rollbar_configured:
            rollbar.report_exc_info()
        raise
