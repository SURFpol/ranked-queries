#!/usr/bin/env python
import json
import re
from itertools import chain

import click
import requests


ENDPOINT = "https://surfpol.sda.surf-hosted.nl"
USERNAME = ""
PASSWORD = ""
INDEX = "freeze-1"

ES_METRIC = {
    'precision': {
        'k': 20,
        'relevant_rating_threshold': 1,
        'ignore_unlabeled': False           
    }
}


def load_json(path):
    with open(path) as stream:
        return json.load(stream)


def format_multi_match(query_text):
    return [
        {
            "multi_match": {
                "fields": [
                    "text.en",
                    "text.nl",
                    "title"
                ],
                "fuzziness": 0,
                "operator": "or",
                "query": query_text,
                "type": "best_fields"
            }
        },
        {
            "multi_match": {
                "fields": [
                    "text.en",
                    "text.nl",
                    "title"
                ],
                "operator": "or",
                "query": query_text,
                "type": "phrase_prefix"
            }
        }
    ]


def format_requests(query):
    requests = []
    for item in query['queries']:
        request = {
            'id': re.sub(r'\s+', '-', item),
            'request': {
                'query': {
                    'bool': {
                        'minimum_should_match': 1,
                        'should': format_multi_match(item)
                    }
                }   
            },
            'ratings': [
                {'_index': INDEX, '_id': doc['hash'], 'rating': doc['rating']}
                for doc in query['items']
            ]
        }
        requests.append(request)
    return requests


@click.command()
@click.argument('queries', type=load_json)
def main(queries):
    body = {
        'requests': list(chain(*[format_requests(_) for _ in queries])),
        'metric': ES_METRIC
    }
    result = requests.get(
        '{}/{}/_rank_eval'.format(ENDPOINT, INDEX),
        auth=(USERNAME, PASSWORD),
        json=body
    )
    print(result.text)


if __name__ == '__main__':
    main()
