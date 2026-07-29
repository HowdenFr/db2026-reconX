import os
import shutil

def move(src, dst):
    if os.path.exists(src):
        print('moving', src, '->', dst)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.move(src, dst)
    else:
        print('missing', src)

base = os.path.abspath(os.path.dirname(__file__))
source = os.path.join(base, 'src', 'main', 'java', 'com', 'dbtraining', 'reconx')
map_dirs = {
    'dto': os.path.join(base, 'common', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'dto'),
    'exception': os.path.join(base, 'common', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'exception'),
    'model': os.path.join(base, 'domain', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'model'),
    'repository': os.path.join(base, 'repository', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'repository'),
    'service': os.path.join(base, 'service', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'service'),
    'kafka': os.path.join(base, 'service', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'kafka'),
    'controller': os.path.join(base, 'api', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'controller'),
    'config': os.path.join(base, 'api', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'config'),
    'observability': os.path.join(base, 'api', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'observability'),
    'security': os.path.join(base, 'api', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'security'),
}
for name, dest in map_dirs.items():
    move(os.path.join(source, name), dest)

app_src = os.path.join(source, 'ReconxApplication.java')
app_dst = os.path.join(base, 'api', 'src', 'main', 'java', 'com', 'dbtraining', 'reconx', 'ReconxApplication.java')
move(app_src, app_dst)

res_src = os.path.join(base, 'src', 'main', 'resources')
res_dst = os.path.join(base, 'api', 'src', 'main', 'resources')
if os.path.exists(res_src):
    if os.path.exists(res_dst):
        for item in os.listdir(res_src):
            move(os.path.join(res_src, item), os.path.join(res_dst, item))
        os.rmdir(res_src)
    else:
        shutil.move(res_src, res_dst)

print('done')
