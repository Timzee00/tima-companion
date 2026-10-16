import ast,time,json,base64
from pathlib import Path
from unittest.mock import Mock,MagicMock
s=Path(__file__).with_name('tima.py').read_text();t=ast.parse(s)
node=next(n for n in t.body if isinstance(n,ast.FunctionDef) and n.name=='_tg_media')
g=dict(time=time,Path=Path,json=json,base64=base64,_TG_MAX_FILE=10*1024*1024,_tg_api=Mock(return_value={'file_path':'voice/file.ogg'}),_req=MagicMock(),transcribe_voice=Mock(return_value=('Send Merit hello',None)),process=Mock(return_value='Action result'),_tg_answer_with_evidence=Mock(return_value='Public answer'),GeneratedImageResult=type('GeneratedImageResult',(str,),{}))
import threading
g['_tg_owner_lock']=threading.Lock();g['_tg_is_owner']=lambda msg:msg['from']['id']==7 and not msg.get('forward_origin')
response=g['_req'].get.return_value.__enter__.return_value;response.iter_content.return_value=[b'ogg']
exec(compile(ast.Module(body=[node],type_ignores=[]),'voice','exec'),g)
msg={'chat':{'id':7,'type':'private'},'from':{'id':7},'date':time.time(),'voice':{'file_id':'test','mime_type':'audio/ogg','duration':3}}
assert 'Heard:' in g['_tg_media']('dummy',msg,'cli','system')
assert g['process'].call_args.kwargs['source']=='owner'
g['process'].reset_mock();msg['from']['id']=8;g['_tg_media']('dummy',msg,'public8','system')
assert not g['process'].called and g['_tg_answer_with_evidence'].called
msg['from']['id']=7;msg['date']=time.time()-500
assert 'expired' in g['_tg_media']('dummy',msg,'cli','system') and not g['process'].called
msg['date']=time.time();g['transcribe_voice'].return_value=(None,'unclear')
assert g['_tg_media']('dummy',msg,'cli','system')=='unclear' and not g['process'].called
print('Passed: owner voice execution, public voice isolation, expired command rejection, unclear transcription rejection.')
