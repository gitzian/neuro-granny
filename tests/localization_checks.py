#!/usr/bin/env python3
"""Verify that every supported locale declares the same UI resource keys.

Plural quantities vary by language and intentionally need not match English.
"""
from collections import Counter
from pathlib import Path
import re
import xml.etree.ElementTree as ElementTree

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res'
BASE = ROOT / 'values/strings.xml'
LOCALES = (
    'values-de', 'values-es', 'values-fr', 'values-it',
    'values-pl', 'values-b+pt+BR', 'values-nl'
)
PLACEHOLDERS = re.compile(r'%(?:\d+\$)?[dsf]')


def read(path):
    entries = ElementTree.parse(path).getroot()
    names = [entry.get('name') for entry in entries if entry.get('name')]
    duplicates = [name for name, count in Counter(names).items() if count > 1]
    if duplicates:
        raise AssertionError(f'{path}: duplicate resources: {duplicates}')
    result = {entry.get('name'): entry for entry in entries if entry.get('name')}
    for name, entry in result.items():
        if entry.tag == 'string' and not (entry.text or '').strip():
            raise AssertionError(f'{path}: empty string: {name}')
    return result


def format_arguments(value):
    return sorted(PLACEHOLDERS.findall(value or ''))


baseline = read(BASE)
for locale in LOCALES:
    path = ROOT / locale / 'strings.xml'
    strings = read(path)
    missing = sorted(set(baseline) - set(strings))
    extra = sorted(set(strings) - set(baseline))
    if missing or extra:
        raise AssertionError(f'{locale}: missing={missing}, extra={extra}')

    for name, english in baseline.items():
        translated = strings[name]
        if english.tag != translated.tag:
            raise AssertionError(f'{locale}: resource type differs: {name}')
        if english.tag == 'string':
            if format_arguments(english.text) != format_arguments(translated.text):
                raise AssertionError(f'{locale}: format placeholders differ: {name}')
        elif english.tag == 'plurals':
            counts = {item.get('quantity') for item in translated}
            if 'other' not in counts:
                raise AssertionError(f'{locale}: plural lacks other: {name}')
            for item in translated:
                # The English long and short forms all use the same formatting arguments.
                english_other = next(x for x in english if x.get('quantity') == 'other')
                if format_arguments(english_other.text) != format_arguments(item.text):
                    raise AssertionError(f'{locale}: plural format differs: {name}')

    if strings['version_unknown'].text == baseline['version_unknown'].text:
        raise AssertionError(f'{locale}: unknown-version text still English')

print(f'Localization checks passed: {len(baseline)} resources in {len(LOCALES) + 1} languages')
