#!/usr/bin/env python

# -*- coding: utf-8 -*-

# Copyright (C) 2018 University of Dundee & Open Microscopy Environment.
# All rights reserved.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

# POST JSON requests to MAPR data microservice
# author: m.t.b.carroll@dundee.ac.uk

import requests

queries = [
    {
        'select': ['value',
                   'count distinct image',
                   'count distinct screen',
                   'count distinct project'],
        'where': ['and',
                  ['name', 'in', ['Gene Symbol']],
                  ['lower value', 'like', ['%fermt%']],
                  ['ns', 'in', ['openmicroscopy.org/mapr/gene']],
                  ['or',
                   ['and',
                    ['project', 'is', []],
                    ['screen', 'not is', []]],
                   ['and',
                    ['project', 'not is', []],
                    ['screen', 'is', []]]]],
        'group': ['value'],
        'order': ['count distinct image desc']
    }, {
        'select': ['plate',
                   'count distinct field'],
        'where': ['and',
                  ['name', 'in', ['siRNA Identifier',
                                  'siRNA Pool Identifier']],
                  ['ns', 'in', ['openmicroscopy.org/mapr/sirna']],
                  ['value', '=', ['siCONTROL']],
                  ['screen', '=', [206]]],
        'group': ['plate'],
        'limit': 8
    }, {
        'select': ['count distinct value'],
        'where': ['and',
                  ['name', 'in', ['Organism']],
                  ['value', '!=', ['']],
                  ['ns', 'in', ['openmicroscopy.org/mapr/organism']],
                  ['or',
                   ['field', 'not is', []],
                   ['dataset', 'not is', []]]]
    }, {
        'select': ['project', 'count distinct annotation'],
        'where': ['and',
                  ['ns', '=', ['openmicroscopy.org/mapr/phenotype']],
                  ['project', 'not is', []]],
        'group': ['project']
    }, {
        "select": ["value"],
        "where": ["and",
	          ["ns", "in", ["openmicroscopy.org/mapr/gene"]],
	          ["name", "in", ["Gene Symbol"]],
	          ["lower value", "like", ["ash2%"]]],
        'group': ['value'],
        "order": ["length value", "lower value"],
        "limit": 500
    }
]

for query in queries:
    response = requests.post('http://localhost:8080/mapr', json=query)

    if response.status_code != 200:
        print('{}: {}'.format(response.status_code, response.reason))
    else:
        for row in response.json():
            print('\t'.join(map(str, row)))
        print('elapsed time: {}\n'.format(response.elapsed))
