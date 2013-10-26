import os
import subprocess
import shutil
import glob
import pprint

if GetOption('clean'):
  Execute('rm -rf bin gen/src/*')
  exit()

def convert(word):
  return ''.join(x.capitalize() or '_' for x in word.split('_'))

gen_dir = 'gen'
gen_src_dir = os.path.join('gen', 'src')
gen_src_glob = os.path.join(gen_src_dir, '*')

boxroyal_package_dir = os.path.join('ch', 'nevill', 'boxroyal')
proto_dir = os.path.join('src', boxroyal_package_dir, 'proto')

gen_contents = glob.glob(gen_src_glob)
if gen_contents:
  subprocess.check_call(['rm', '-r'] + gen_contents)

subprocess.check_call(['protoc', '--java_out=%s' % 'gen/src', '--proto_path=%s' % proto_dir] + glob.glob(os.path.join(proto_dir, '*.proto')))

environment = Environment(JAVACLASSPATH = glob.glob(os.path.join('lib', '*', '*.jar')), JAVASOURCEPATH = '.:src:gen/src')

environment.Java(target='bin', source='.')
