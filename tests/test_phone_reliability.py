import ast,re,json,time,threading,sqlite3,tempfile,os,hashlib
from pathlib import Path
from datetime import datetime,timedelta
from unittest.mock import Mock
from typing import Optional,List,Dict
source=Path(__file__).with_name('tima.py').read_text();tree=ast.parse(source)
names={'_read_battery','get_battery','battery_label','get_battery_pct','_contact_name','_get_contacts','_contact_candidates','resolve_contact','WhatsAppResult','_wa_screen','_wa_click','_wa_wait','_wa_header','_wa_send','send_whatsapp','whatsapp_schedule','whatsapp_jobs','_run_whatsapp_jobs','_recover_whatsapp_jobs','parse_time','transcribe_voice','_needs_search','web_search','_web_search_with_sources','_tg_is_owner'}
nodes=[n for n in tree.body if isinstance(n,(ast.FunctionDef,ast.ClassDef)) and n.name in names]
g=dict(re=re,json=json,threading=threading,os=os,Path=Path,datetime=datetime,timedelta=timedelta,Optional=Optional,List=List,Dict=Dict,time=Mock(wraps=time),_contacts=[],_contacts_t=0,_contact_error='',shutil=Mock(),sh_safe=Mock(),is_termux=lambda:True,_termux_api_ok=lambda:None,wset=Mock(),_battery_lock=threading.Lock(),_battery_sample={'time':0},_control_lock=threading.Lock(),bridge_rpc=Mock(),open_app=Mock(),OWNER_CHAT_ID='7',REQUESTS_OK=True,ALL_KEYS=['gsk_dummy'],_req=Mock(),_SEARCH_SKIP_RE=re.compile(r'^(open|set|generate)',re.I),_SEARCH_TRIGGER_RE=re.compile(r'latest|current|weather|news',re.I))
g['time'].sleep=Mock()
exec(compile(ast.Module(body=nodes,type_ignores=[]),'phone','exec'),g)
g['sh_safe'].return_value=(0,json.dumps({'percentage':91,'status':'DISCHARGING'}),'')
assert g['get_battery']().startswith('Battery: 91%')
g['sh_safe'].return_value=(0,'{}','');assert 'unavailable' in g['get_battery']()
g['sh_safe'].return_value=(0,'{"percentage": true}','');assert 'unavailable' in g['get_battery']()
g['sh_safe'].return_value=(0,'{"percentage": 101}','');assert 'unavailable' in g['get_battery']()
g['_contacts']=[{'name':'Merit School','number':'+2349012345678'},{'name':'Merit Work','number':'+2349012345679'}];g['_contacts_t']=time.time()
assert len(g['_contact_candidates']('Merit'))==2
assert len(g['_contact_candidates']('merit school'))==1
store={};g['kget']=lambda k:store.get(k,'');g['kset']=lambda k,v:store.__setitem__(k,v)
assert g['resolve_contact']('Merit')=='Merit'
# Guarded send: actual click path is verified from mock accessibility states.
g['_get_contacts']=lambda:[]
g['resolve_contact']=lambda contact:'+2349012345678'
g['_contact_candidates']=lambda name:[]
g['sh_safe'].return_value=(0,'Starting','')
def screen(nodes):return {'snapshot':'fresh','package':'com.whatsapp','nodes':nodes}
header={'id':1,'text':'Merit','view_id':'com.whatsapp:id/conversation_contact_name'}
entry={'id':2,'editable':True,'text':'','view_id':'com.whatsapp:id/entry'}
typed=dict(entry,text='Happy birthday!')
send={'id':3,'clickable':True,'description':'Send','view_id':'com.whatsapp:id/send'}
bubble={'id':4,'text':'Happy birthday!','view_id':'com.whatsapp:id/message_text'}
g['bridge_rpc'].side_effect=[{'status':'ready'},screen([header,entry]),{'status':'accepted'},screen([header,typed,send]),{'status':'accepted'},screen([header,entry,bubble])]
result=g['_wa_send']('Merit','Happy birthday!');assert result.status=='submitted',str(result)
assert 'not verified' in result
# Existing draft is never overwritten.
g['bridge_rpc'].side_effect=[{},screen([header,dict(entry,text='Existing draft')])]
assert g['_wa_send']('Merit','Happy birthday!').status=='blocked_draft'
# Wrong recipient must never get a Send action.
g['bridge_rpc'].reset_mock();g['bridge_rpc'].side_effect=[{}]+[screen([dict(header,text='Someone Else'),entry])]*6
assert g['_wa_send']('Merit','Hello').status=='blocked_bridge'
assert not any(c.args[0]=='click' for c in g['bridge_rpc'].call_args_list)
# Lock prevents launching WhatsApp.
g['bridge_rpc'].side_effect=ValueError('Unlock phone manually.');g['sh_safe'].reset_mock()
assert g['_wa_send']('Merit','Hello').status=='blocked_locked';assert not g['sh_safe'].called
# Exception after Send never reports success and is not eligible for automatic retry.
g['bridge_rpc'].side_effect=[{},screen([header,entry]),{},screen([header,typed,send]),{},ValueError('connection lost')]
assert g['_wa_send']('Merit','Happy birthday!').status=='unknown'
# Real SQLite job claim, waiting, expiration and restart behavior.
clock=[datetime(2026,10,13,11,0)]
g['now']=lambda:clock[0];g['nows']=lambda:clock[0].strftime('%Y-%m-%d %H:%M:%S')
with tempfile.TemporaryDirectory() as directory:
 db=str(Path(directory)/'jobs.db')
 def conn():
  c=sqlite3.connect(db);c.row_factory=sqlite3.Row;return c
 g['_conn']=conn;g['_db_lock']=threading.Lock();g['add_history']=Mock();g['send_notification']=Mock()
 assert '12:00' in g['whatsapp_schedule']('Merit','Happy birthday!','12pm')
 clock[0]=datetime(2026,10,13,12,0)
 g['_wa_send']=Mock(return_value=g['WhatsAppResult']('Locked','blocked_locked'))
 g['_run_whatsapp_jobs']();assert 'waiting' in g['whatsapp_jobs']()
 g['_wa_send'].return_value=g['WhatsAppResult']('visible','submitted')
 g['_run_whatsapp_jobs']();g['_run_whatsapp_jobs']();assert g['_wa_send'].call_count==2
 assert 'submitted' in g['whatsapp_jobs']()
 g['whatsapp_schedule']('Merit','Hello','in 1 minute');clock[0]+=timedelta(minutes=20)
 g['_run_whatsapp_jobs']();assert 'expired' in g['whatsapp_jobs']()
 g['whatsapp_schedule']('Merit','Hello','in 1 minute')
 c=conn();c.execute("UPDATE whatsapp_jobs SET state='running' WHERE id=3");c.commit();c.close()
 g['_recover_whatsapp_jobs']();assert 'Interrupted' in g['whatsapp_jobs']()
# Voice clear/unclear/provider failure.
r=g['_req'].post.return_value;r.status_code=200;r.json.return_value={'text':'Send Merit hello','segments':[{'avg_logprob':-.1,'no_speech_prob':0}]}
assert g['transcribe_voice'](b'audio')[0]=='Send Merit hello'
r.json.return_value={'text':'Send money','segments':[{'avg_logprob':-2}]}
assert g['transcribe_voice'](b'audio')[0] is None
r.status_code=429;assert '429' in g['transcribe_voice'](b'audio')[1]
# Current facts cannot silently fall back to old model knowledge.
g['_fetch_search_snippets']=lambda q:'';g['ask_llm']=Mock();g['_search_url']=lambda s:s if s.startswith('https://') else ''
assert 'cannot verify' in g['web_search']('current weather');assert not g['ask_llm'].called
assert g['_needs_search']('show latest news')
assert not g['_needs_search']('by 12pm today send Merit a birthday message')
assert not g['_needs_search']('/whatsapp-at tomorrow at 12pm | Merit | hi')
owner={'chat':{'type':'private','id':7},'from':{'id':7}}
assert g['_tg_is_owner'](owner)
assert not g['_tg_is_owner'](dict(owner,forward_origin={'type':'user'}))
print('Passed: measured/invalid battery, contact ambiguity, recipient/draft checks, Send readback, locked and uncertain-send outcomes, SQLite schedule retries/expiry/recovery, voice confidence/errors, current-source failure, owner authentication.')
