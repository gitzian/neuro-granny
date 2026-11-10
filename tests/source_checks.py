#!/usr/bin/env python3
"""Static contract checks available without an Android SDK.
Does NOT replace Android lint, an emulator run, or an APK build.
"""
from pathlib import Path
from xml.etree import ElementTree as ET
import re, sqlite3, tempfile
ROOT = Path(__file__).resolve().parents[1]
java = ROOT/'app/src/main/java'
manifest = ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
permissions = [el.get(android+'name') for el in manifest.findall('uses-permission')]
assert 'android.permission.INTERNET' not in permissions
assert 'android.permission.RECEIVE_BOOT_COMPLETED' in permissions
app = manifest.find('application')
assert app.get(android+'allowBackup') == 'false'
files = list(java.rglob('*.java'))
assert len(files) >= 19
source = '\n'.join(p.read_text() for p in files)
assert 'setOngoing(true)' in source
assert 'setAndAllowWhileIdle' in source
assert 'Math.floorMod(' not in source
assert 'java.util.function.Consumer' not in source
assert 'Argon2Parameters.ARGON2_id' in source
assert 'RENDERMODE_WHEN_DIRTY' in source
# Screenshots must remain available for users and release documentation.
assert 'FLAG_SECURE' not in source, 'FLAG_SECURE would block app screenshots'
assert 'BOOT_COMPLETED' in source
assert 'GRADUATION_EVERY = 5' in source
assert 'GRADUATION_EVERY' in source
# Compose SQL strings from schema class to validate with standard SQLite.
schema = (java/'org/gitzian/neurogranny/data/NeuroDatabase.java').read_text()
commands=[]
for match in re.finditer(r'db\.execSQL\((.*?)\);',schema,re.S):
    sql=''.join(re.findall(r'"([^"]*)"',match.group(1)))
    if sql.startswith('CREATE '): commands.append(sql)
assert len(commands) == 4, commands
with sqlite3.connect(':memory:') as db:
    db.execute('PRAGMA foreign_keys=ON')
    for sql in commands: db.execute(sql)
    cols=[x[1] for x in db.execute('PRAGMA table_info(memories)')]
    assert 'plain' in cols and 'verifier' in cols and 'pattern_seed' in cols
    valid=("Test",0,"plaintext","",1,86400,3600,1209600,3600,0,0,123,1,0,1000,1000)
    fields='title,mode,plain,verifier,schedule_mode,fixed_seconds,min_seconds,max_seconds,current_seconds,next_due,is_due,pattern_seed,pattern_version,successes,created_at,updated_at'
    db.execute('INSERT INTO memories ('+fields+') VALUES ('+','.join('?'*len(valid))+')',valid)
    db.execute("INSERT INTO review_events(memory_id,timestamp,correct,wrong_attempts,confidence,interval_seconds) VALUES (1,1000,0,1,'',3600)")
    db.execute('DELETE FROM memories WHERE _id=1')
    assert db.execute('SELECT count(*) FROM review_events').fetchone()[0] == 0
    try:
        bad=list(valid);bad[1]=1;db.execute('INSERT INTO memories ('+fields+') VALUES ('+','.join('?'*len(bad))+')',bad)
        raise AssertionError('CHECK constraint failed to reject invalid verifier state')
    except sqlite3.IntegrityError: pass
print(f'SOURCE CHECKS PASSED: {len(files)} Java files, 4 schema statements, SQLite constraints and manifest')
