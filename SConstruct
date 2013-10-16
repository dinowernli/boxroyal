import os
import subprocess
import shutil
import glob

if GetOption('clean'):
  Execute('rm -rf bin gen')
  exit()

def convert(word):
    return ''.join(x.capitalize() or '_' for x in word.split('_'))

gen_dir = 'gen'

boxroyal_package_dir = os.path.join('ch', 'nevill', 'boxroyal')
proto_dir = os.path.join('src', boxroyal_package_dir, 'proto')

gen_contents = glob.glob(os.path.join(gen_dir, '*'))
if gen_contents:
  subprocess.call(['rm', '-r'] + glob.glob(os.path.join(gen_dir, '*')))

shutil.copytree('src', os.path.join(gen_dir, 'src'))
subprocess.call(['protoc', '--java_out=%s' % 'gen/src', '--proto_path=%s' % proto_dir] + glob.glob(os.path.join(proto_dir, '*.proto')))

environment = Environment(JAVACLASSPATH = glob.glob(os.path.join('lib', '*', '*.jar')), JAVASOURCEPATH = '.')
environment.Java(target='bin', source=gen_dir)
